@file:Suppress("unused")
package com.github.cao.awa.cason.serialize.parser

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.array.JSONArray
import com.github.cao.awa.cason.exception.NeedMoreInputException
import com.github.cao.awa.cason.number.CasonNumber
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.primary.JSONBoolean
import com.github.cao.awa.cason.primary.JSONNull
import com.github.cao.awa.cason.primary.JSONNumber
import com.github.cao.awa.cason.primary.JSONString
import com.github.cao.awa.cason.reader.CharReader
import com.github.cao.awa.cason.util.CasonUtil
import java.math.BigDecimal
import java.math.BigInteger

object JSONParser {
    fun parseObject(input: String): JSONObject = parse(input) as JSONObject
    fun parseArray(input: String): JSONArray = parse(input) as JSONArray

    fun parse(input: String): JSONElement {
        val chars = input.toCharArray()
        val reader = CharReader(chars, 0, chars.size, true)
        val element = reader.parseElement()
        reader.skipWsAndCommentsFast()
        if (reader.eof()) {
            return element
        } else {
            reader.error("Trailing characters after top-level value")
        }
    }

    private fun CharReader.ensureAvailable(n: Int = 1) {
        if (this.index + (n - 1) >= this.end) {
            if (this.isFinal) {
                error("Unexpected EOF")
            }
            throw NeedMoreInputException(this)
        }
    }

    private fun CharReader.peekChar(): Char? =
        if (this.index < end) this.chars[this.index] else null

    private fun CharReader.peek2Char(): Char? =
        if (this.index + 1 < end) this.chars[this.index + 1] else null

    private fun CharReader.readCharNoLine(): Char {
        // Fast path: for structural / number / identifier scanning (no line terminators expected).
        ensureAvailable(1)
        val c = this.chars[this.index++]
        this.col++
        return c
    }

    private fun CharReader.expectChar(ch: Char) {
        ensureAvailable(1)
        val got = readCharNoLine()
        if (got != ch) {
            error("Expected '$ch' but got '$got'")
        }
    }

    private fun CharReader.parseElement(): JSONElement {
        skipWsAndCommentsFast()
        val index = this.index
        if (index >= this.end) {
            if (this.isFinal) {
                error("Expected a value but got EOF")
            }
            throw NeedMoreInputException(this)
        }
        return when (this.chars[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"', '\'' -> JSONString(parseString())
            '+', '-', '.', in '0'..'9' -> JSONNumber(parseNumber())
            else -> parseIdentifierValueOrError(this.chars[index])
        }
    }

    private fun CharReader.parseIdentifierValueOrError(c: Char): JSONElement {
        if (CasonUtil.isIdStart(c)) {
            return when (val id = parseIdentifierFast()) {
                "null" -> JSONNull
                "true" -> JSONBoolean(true)
                "false" -> JSONBoolean(false)
                "Infinity" -> JSONNumber(CasonNumber.posInf())
                "NaN" -> JSONNumber(CasonNumber.nan())
                else -> error("Unexpected identifier '$id'")
            }
        }
        error("Unexpected character '$c'")
    }

    private fun CharReader.parseObject(): JSONObject {
        expectChar('{')
        skipWsAndCommentsFast()

        val map = LinkedHashMap<String, JSONElement>(32)

        if (peekChar() == '}') {
            readCharNoLine()
            return JSONObject(map)
        }

        while (true) {
            val key = parseObjectKeyFast()
            skipWsAndCommentsFast()
            // ':' is structural, no line break expected.
            expectChar(':')
            val value = parseElement()
            map[key] = value
            skipWsAndCommentsFast()

            when (peekChar()) {
                ',' -> {
                    readCharNoLine()
                    skipWsAndCommentsFast()
                }
                '}' -> {
                    readCharNoLine()
                    break
                }
                else -> error("Expected ',' or '}' in object")
            }
        }

        return JSONObject(map)
    }

    private fun CharReader.parseObjectKeyFast(): String {
        skipWsAndCommentsFast()
        val index = this.index
        if (index >= this.end) {
            error("Unexpected EOF in object key")
        }
        val c = this.chars[index]
        return when {
            c == '"' || c == '\'' -> parseString()
            c == '+' || c == '-' || c == '.' || c.isDigit() -> parseNumber().toString()
            CasonUtil.isIdStart(c) -> parseIdentifierFast()
            else -> error("Invalid object key start '$c'")
        }
    }

    private fun CharReader.parseArray(): JSONArray {
        expectChar('[')
        skipWsAndCommentsFast()

        val list = ArrayList<JSONElement>(32)

        if (peekChar() == ']') {
            readCharNoLine()
            return JSONArray(list)
        }

        while (true) {
            list.add(parseElement())
            skipWsAndCommentsFast()

            when (peekChar()) {
                ',' -> {
                    readCharNoLine()
                    skipWsAndCommentsFast()
                }
                ']' -> {
                    readCharNoLine()
                    break
                }
                else -> error("Expected ',' or ']' in array")
            }
        }

        return JSONArray(list)
    }

    fun CharReader.skipWsAndCommentsFast() {
        val a = chars
        var i = index
        val e = end

        var line = this.line
        var col = this.col

        fun stepLineTerminatorAt(pos: Int): Int {
            val ch = a[pos]
            return when (ch) {
                '\n', '\u2028', '\u2029' -> {
                    line++
                    col = 1
                    pos + 1
                }
                '\r' -> {
                    // Treat \r\n as one line break when possible.
                    if (pos + 1 < e && a[pos + 1] == '\n') {
                        line++
                        col = 1
                        pos + 2
                    } else {
                        if (pos + 1 >= e && !this.isFinal) throw NeedMoreInputException(this)
                        line++
                        col = 1
                        pos + 1
                    }
                }
                else -> {
                    // Not a terminator.
                    col++
                    pos + 1
                }
            }
        }

        while (true) {
            // Skip whitespace or line terminators.
            while (i < e) {
                val c = a[i]
                if (CasonUtil.isWs(c)) {
                    i++
                    col++
                    continue
                }
                if (CasonUtil.isLineTerminator(c)) {
                    i = stepLineTerminatorAt(i)
                    continue
                }
                break
            }

            // Line comment '//' .
            if (i + 1 < e && a[i] == '/' && a[i + 1] == '/') {
                i += 2
                col += 2
                while (i < e && !CasonUtil.isLineTerminator(a[i])) {
                    i++
                    col++
                }
                // Do not consume line terminator here, loop will handle it.
                continue
            }

            // Block comment '/* ... */' .
            if (i + 1 < e && a[i] == '/' && a[i + 1] == '*') {
                i += 2
                col += 2
                while (true) {
                    if (i + 1 >= e) {
                        if (this.isFinal) {
                            error("Unterminated block comment")
                        }
                        throw NeedMoreInputException(this)
                    }
                    val commentChar = a[i]
                    if (commentChar == '*' && a[i + 1] == '/') {
                        i += 2
                        col += 2
                        break
                    }
                    if (CasonUtil.isLineTerminator(commentChar)) {
                        i = stepLineTerminatorAt(i)
                    } else {
                        i++
                        col++
                    }
                }
                continue
            }

            break
        }

        this.index = i
        this.line = line
        this.col = col
    }

    private fun CharReader.parseString(): String {
        ensureAvailable(1)
        val quote = this.chars[this.index]
        // consume quote
        this.index++
        this.col++

        val a = this.chars
        val start = this.index
        var i = start
        val e = this.end

        // Fast scan: no escapes, no line terminators.
        while (true) {
            if (i >= e) {
                if (this.isFinal) error("Unterminated string")
                throw NeedMoreInputException(this)
            }
            val c = a[i]
            if (c == quote) {
                // Commit index.
                this.index = i + 1
                this.col += (i - start) + 1
                return String(a, start, i - start)
            }
            if (c == '\\') break
            if (c == '\n' || c == '\r' || c == '\u2028' || c == '\u2029') {
                error("Unescaped line terminator in string")
            }
            i++
        }

        // Slow path: has escape.
        val builder = StringBuilder((i - start) + 16)
        builder.appendRange(a, start, i)
        // Move reader to the backslash.
        this.col += (i - start)
        this.index = i

        while (true) {
            ensureAvailable(1)
            val c = this.chars[this.index++]
            this.col++

            when (c) {
                quote -> return builder.toString()
                '\\' -> {
                    ensureAvailable(1)
                    val escapeChar = this.chars[this.index++]
                    this.col++
                    builder.append(
                        when (escapeChar) {
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            'b' -> '\b'
                            'f' -> '\u000C'
                            '"' -> '"'
                            '\'' -> '\''
                            '\\' -> '\\'
                            else -> error("Unknown escape \\$escapeChar")
                        }
                    )
                }
                '\n', '\r', '\u2028', '\u2029' -> error("Unescaped line terminator in string")
                else -> builder.append(c)
            }
        }
    }

    private fun CharReader.parseNumber(): CasonNumber {
        var sign = 1
        ensureAvailable(1)
        val c = this.chars[this.index]

        if (c == '+' || c == '-') {
            sign = if (c == '-') -1 else 1
            this.index++
            this.col++
            if (this.index >= this.end) {
                if (this.isFinal) {
                    error("Invalid number")
                }
                throw NeedMoreInputException(this)
            }
        }

        var mantissa = 0L
        var mantissaDigits = 0
        var exp10 = 0
        var overflow = false

        val a = chars
        val e = end

        // Integer part.
        while (this.index < e) {
            val ch = a[this.index]
            if (ch !in '0'..'9') {
                break
            }
            val d = ch.code - 48
            if (!overflow) {
                val n = mantissa * 10 + d
                if (n < mantissa) overflow = true else mantissa = n
            }
            mantissaDigits++
            this.index++
            this.col++
        }

        // Fraction.
        if (this.index < e && a[this.index] == '.') {
            this.index++
            this.col++
            while (this.index < e) {
                val ch = a[this.index]
                if (ch !in '0'..'9') {
                    break
                }
                val d = ch.code - 48
                if (!overflow) {
                    val n = mantissa * 10 + d
                    if (n < mantissa) overflow = true else mantissa = n
                }
                mantissaDigits++
                exp10--
                this.index++
                this.col++
            }
        }

        if (mantissaDigits == 0) {
            error("Invalid number")
        }

        // Exponent.
        if (this.index < e) {
            val ch = a[this.index]
            if (ch == 'e' || ch == 'E') {
                this.index++
                this.col++

                ensureAvailable(1)
                var expSign = 1
                if (this.index < e) {
                    val s = a[this.index]
                    if (s == '+' || s == '-') {
                        expSign = if (s == '-') -1 else 1
                        this.index++
                        this.col++
                    }
                }

                var exp = 0
                var expDigits = 0
                while (this.index < e) {
                    val dch = a[this.index]
                    if (dch !in '0'..'9') {
                        break
                    }
                    exp = exp * 10 + (dch.code - 48)
                    expDigits++
                    this.index++
                    this.col++
                }
                if (expDigits == 0) error("Invalid exponent in number")
                exp10 += expSign * exp
            }
        }

        val bd = if (!overflow && mantissaDigits <= 19) {
            BigDecimal.valueOf(sign * mantissa).scaleByPowerOfTen(exp10)
        } else {
            var bi = BigInteger.valueOf(mantissa)
            if (sign < 0) {
                bi = bi.negate()
            }
            BigDecimal(bi).scaleByPowerOfTen(exp10)
        }

        return CasonNumber.finite(bd)
    }

    private fun CharReader.parseIdentifierFast(): String {
        val a = this.chars
        val start = this.index

        // first char must exist and be idStart (caller checked)
        ensureAvailable(1)
        this.index++
        this.col++

        var i = this.index
        val e = this.end
        while (i < e && CasonUtil.isIdPart(a[i])) i++

        // Commit index.
        val len = i - start
        val extra = i - this.index
        this.index = i
        this.col += extra

        return String(a, start, len)
    }
}

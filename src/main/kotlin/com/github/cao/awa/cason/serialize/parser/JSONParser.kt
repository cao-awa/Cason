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
        reader.skipWsAndComments()
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
        skipWsAndComments()
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
            return when (val id = parseIdentifier()) {
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
        skipWsAndComments()

        val map = LinkedHashMap<String, JSONElement>(32)

        if (peekChar() == '}') {
            readCharNoLine()
            return JSONObject(map)
        }

        while (true) {
            val key = parseObjectKey()
            skipWsAndComments()
            // ':' is structural, no line break expected.
            expectChar(':')
            val value = parseElement()
            map[key] = value
            skipWsAndComments()

            when (peekChar()) {
                ',' -> {
                    readCharNoLine()
                    skipWsAndComments()
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

    private fun CharReader.parseObjectKey(): String {
        skipWsAndComments()
        val index = this.index
        if (index >= this.end) {
            error("Unexpected EOF in object key")
        }
        val keyChar = this.chars[index]
        return when {
            keyChar == '"' || keyChar == '\'' -> parseString()
            keyChar == '+' || keyChar == '-' || keyChar == '.' || keyChar.isDigit() -> parseNumber().toString()
            CasonUtil.isIdStart(keyChar) -> parseIdentifier()
            else -> error("Invalid object key start '$keyChar'")
        }
    }

    private fun CharReader.parseArray(): JSONArray {
        expectChar('[')
        skipWsAndComments()

        val list = ArrayList<JSONElement>(32)

        if (peekChar() == ']') {
            readCharNoLine()
            return JSONArray(list)
        }

        while (true) {
            list.add(parseElement())
            skipWsAndComments()

            when (peekChar()) {
                ',' -> {
                    readCharNoLine()
                    skipWsAndComments()
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

    fun CharReader.skipWsAndComments() {
        val chars = this.chars
        var index = this.index
        val end = this.end

        var line = this.line
        var col = this.col

        fun stepLineTerminatorAt(pos: Int): Int {
            val ch = chars[pos]
            return when (ch) {
                '\n', '\u2028', '\u2029' -> {
                    line++
                    col = 1
                    pos + 1
                }
                '\r' -> {
                    // Treat \r\n as one line break when possible.
                    if (pos + 1 < end && chars[pos + 1] == '\n') {
                        line++
                        col = 1
                        pos + 2
                    } else {
                        if (pos + 1 >= end && !this.isFinal) throw NeedMoreInputException(this)
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
            while (index < end) {
                val espaceChar = chars[index]
                if (CasonUtil.isWs(espaceChar)) {
                    index++
                    col++
                    continue
                }
                if (CasonUtil.isLineTerminator(espaceChar)) {
                    index = stepLineTerminatorAt(index)
                    continue
                }
                break
            }

            // Line comment '//' .
            if (index + 1 < end && chars[index] == '/' && chars[index + 1] == '/') {
                index += 2
                col += 2
                while (index < end && !CasonUtil.isLineTerminator(chars[index])) {
                    index++
                    col++
                }
                // Do not consume line terminator here, loop will handle it.
                continue
            }

            // Block comment '/* ... */' .
            if (index + 1 < end && chars[index] == '/' && chars[index + 1] == '*') {
                index += 2
                col += 2
                while (true) {
                    if (index + 1 >= end) {
                        if (this.isFinal) {
                            error("Unterminated block comment")
                        }
                        throw NeedMoreInputException(this)
                    }
                    val commentChar = chars[index]
                    if (commentChar == '*' && chars[index + 1] == '/') {
                        index += 2
                        col += 2
                        break
                    }
                    if (CasonUtil.isLineTerminator(commentChar)) {
                        index = stepLineTerminatorAt(index)
                    } else {
                        index++
                        col++
                    }
                }
                continue
            }

            break
        }

        this.index = index
        this.line = line
        this.col = col
    }

    private fun CharReader.parseString(): String {
        val chars = this.chars
        ensureAvailable(1)
        val quote = chars[this.index]
        this.index++
        this.col++

        val start = this.index
        var index = start
        val end = this.end

        // Fast path: only look for quote or backslash.
        while (true) {
            if (index >= end) {
                if (this.isFinal) error("Unterminated string")
                throw NeedMoreInputException(this)
            }
            val c = chars[index]
            if (c == quote) {
                this.index = index + 1
                this.col += (index - start) + 1
                return String(chars, start, index - start)
            }
            if (c == '\\') break
            index++
        }

        // Slow path: escape or invalid content exists.
        val builder = StringBuilder((index - start) + 16)
        builder.appendRange(chars, start, index)

        // Commit reader to first backslash.
        this.col += (index - start)
        this.index = index

        var idx = this.index
        var col = this.col

        while (true) {
            if (idx >= end) {
                if (this.isFinal) error("Unterminated string")
                throw NeedMoreInputException(this)
            }

            val c = chars[idx++]
            col++

            when (c) {
                quote -> {
                    this.index = idx
                    this.col = col
                    return builder.toString()
                }

                '\\' -> {
                    if (idx >= end) {
                        if (this.isFinal) error("Unterminated escape in string")
                        throw NeedMoreInputException(this)
                    }
                    val esc = chars[idx++]
                    col++
                    builder.append(
                        when (esc) {
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            'b' -> '\b'
                            'f' -> '\u000C'
                            '"' -> '"'
                            '\'' -> '\''
                            '\\' -> '\\'
                            else -> error("Unknown escape \\$esc")
                        }
                    )
                }

                // Line terminators are illegal unless escaped
                '\n', '\u2028', '\u2029' ->
                    error("Unescaped line terminator in string")

                '\r' -> {
                    // If CR is last char in buffer and streaming, might be CRLF split
                    if (idx >= end && !this.isFinal) throw NeedMoreInputException(this)
                    error("Unescaped line terminator in string")
                }

                else -> builder.append(c)
            }
        }
    }

    private fun CharReader.parseNumber(): CasonNumber {
        val a = this.chars
        val e = this.end

        var index = this.index
        var col = this.col

        // Sign.
        var sign = 1
        if (index >= e) {
            if (this.isFinal) {
                error("Invalid number")
            }
            throw NeedMoreInputException(this)
        }
        val first = a[index]
        if (first == '+' || first == '-') {
            sign = if (first == '-') {
                -1
            } else {
                1
            }
            index++
            col++
            if (index >= e) {
                if (this.isFinal) {
                    error("Invalid number")
                }
                throw NeedMoreInputException(this)
            }
        }

        // Mantissa.
        var mantissa = 0L
        var digits = 0
        var exp10 = 0
        var overflow = false

        // integer part
        while (index < e) {
            val c = a[index]
            if (c !in '0'..'9') {
                break
            }
            if (!overflow) {
                val d = c.code - 48
                val n = mantissa * 10 + d
                if (n < mantissa) {
                    overflow = true
                } else {
                    mantissa = n
                }
            }
            digits++
            index++
            col++
        }

        // Fraction.
        if (index < e && a[index] == '.') {
            index++
            col++
            while (index < e) {
                val c = a[index]
                if (c !in '0'..'9') break
                if (!overflow) {
                    val d = c.code - 48
                    val n = mantissa * 10 + d
                    if (n < mantissa) {
                        overflow = true
                    } else {
                        mantissa = n
                    }
                }
                digits++
                exp10--
                index++
                col++
            }
        }

        if (digits == 0) {
            error("Invalid number")
        }

        // Exponent.
        if (index < e) {
            val c = a[index]
            if (c == 'e' || c == 'E') {
                index++
                col++

                if (index >= e) {
                    if (this.isFinal) error("Invalid exponent in number")
                    throw NeedMoreInputException(this)
                }

                var expSign = 1
                val s = a[index]
                if (s == '+' || s == '-') {
                    expSign = if (s == '-') {
                        -1
                    } else {
                        1
                    }
                    index++
                    col++
                }

                var exp = 0
                var expDigits = 0
                while (index < e) {
                    val dch = a[index]
                    if (dch !in '0'..'9') {
                        break
                    }
                    exp = exp * 10 + (dch.code - 48)
                    expDigits++
                    index++
                    col++
                }
                if (expDigits == 0) {
                    error("Invalid exponent in number")
                }
                exp10 += expSign * exp
            }
        }

        // Commit index.
        this.index = index
        this.col = col

        // Materialize number.
        val bd = if (!overflow && digits <= 18) {
            // Fast path: fits in signed Long safely.
            BigDecimal.valueOf(sign * mantissa).scaleByPowerOfTen(exp10)
        } else {
            // Slow path: fallback to BigInteger.
            var bi = BigInteger.valueOf(mantissa)
            if (sign < 0) bi = bi.negate()
            BigDecimal(bi).scaleByPowerOfTen(exp10)
        }

        return CasonNumber.finite(bd)
    }

    private fun CharReader.parseIdentifier(): String {
        val chars = this.chars
        val start = this.index

        // First char must exist and be idStart (caller checked).
        ensureAvailable(1)
        this.index++
        this.col++

        var index = this.index
        val e = this.end
        while (index < e && CasonUtil.isIdPart(chars[index])) {
            index++
        }

        // Commit index.
        val len = index - start
        val extra = index - this.index
        this.index = index
        this.col += extra

        return String(chars, start, len)
    }
}

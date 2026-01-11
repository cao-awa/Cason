@file:Suppress("unused")
package com.github.cao.awa.cason.serialize.parser

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.array.JSONArray
import com.github.cao.awa.cason.exception.JSONParseException
import com.github.cao.awa.cason.exception.NeedMoreInputException
import com.github.cao.awa.cason.primary.number.CasonNumber
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.primary.JSONBoolean
import com.github.cao.awa.cason.primary.JSONNull
import com.github.cao.awa.cason.primary.JSONNumber
import com.github.cao.awa.cason.primary.JSONString
import com.github.cao.awa.cason.util.CasonUtil
import java.math.BigDecimal

open class JSONParser {
    companion object {
        fun parseObject(input: String): JSONObject = parse(input) as JSONObject
        fun parseArray(input: String): JSONArray = parse(input) as JSONArray

        fun parse(input: String): JSONElement {
            val chars = input.toCharArray()
            val parser = JSONParser(0, chars.size, true)
            val element = parser.parseElement(chars)
            if (parser.shouldSkipWs(chars)) {
                parser.skipComments(chars)
            }
            if (parser.eof()) {
                return element
            } else {
                parser.error("Trailing characters after top-level value")
            }
        }
    }

    var index: Int
    val end: Int
    val isFinal: Boolean
    var line: Int = 1
    var col: Int = 1

    constructor(start: Int, end: Int, isFinal: Boolean) {
        this.index = start
        this.end = end
        this.isFinal = isFinal
    }

    fun eof(): Boolean = this.index >= this.end

    protected fun error(msg: String): Nothing {
        throw JSONParseException("$msg at line $this.line, column $this.col")
    }

    protected fun ensureAvailable() {
        if (this.index >= this.end) {
            if (this.isFinal) {
                error("Unexpected EOF")
            }
            throw NeedMoreInputException(this.index, this.line, this.col)
        }
    }

    fun peekChar(chars: CharArray): Char? =
        if (this.index < this.end) chars[this.index] else null

    protected fun readCharNoLine(chars: CharArray): Char {
        // Fast path: for structural / number / identifier scanning (no line terminators expected).
        ensureAvailable()
        val c = chars[this.index++]
        this.col++
        return c
    }

    protected fun expectChar(chars: CharArray, expected: Char) {
        ensureAvailable()
        val got = readCharNoLine(chars)
        if (got != expected) {
            error("Expected '$expected' but got '$got'")
        }
    }

    protected open fun parseElement(chars: CharArray): JSONElement {
        if (shouldSkipWs(chars)) {
            skipComments(chars)
        }
        ensureAvailable()
        return when (val firstChar = chars[this.index]) {
            '{' -> parseObject(chars)
            '[' -> parseArray(chars)
            '"', '\'' -> JSONString(parseString(chars))
            '+', '-', '.', in '0'..'9' -> JSONNumber(parseNumber(chars))
            else -> parseIdentifierValueOrError(chars, firstChar)
        }
    }

    protected fun parseIdentifierValueOrError(chars: CharArray, c: Char): JSONElement {
        if (CasonUtil.isIdStart(c)) {
            return when (val id = parseIdentifier(chars)) {
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

    protected open fun parseObject(chars: CharArray): JSONObject {
        expectChar(chars,'{')
        if (shouldSkipWs(chars)) {
            skipComments(chars)
        }

        val map = LinkedHashMap<String, JSONElement>(24)

        if (peekChar(chars) == '}') {
            readCharNoLine(chars)
            return JSONObject(map)
        }

        while (true) {
            val key = parseObjectKey(chars)
            if (shouldSkipWs(chars)) {
                skipComments(chars)
            }
            // ':' is structural, no line break expected.
            expectChar(chars, ':')
            val value = parseElement(chars)
            map[key] = value
            if (shouldSkipWs(chars)) {
                skipComments(chars)
            }

            when (peekChar(chars)) {
                ',' -> {
                    readCharNoLine(chars)
                    if (shouldSkipWs(chars)) {
                        skipComments(chars)
                    }
                }
                '}' -> {
                    readCharNoLine(chars)
                    break
                }
                else -> error("Expected ',' or '}' in object")
            }
        }

        return JSONObject(map)
    }

    protected open fun parseObjectKey(chars: CharArray): String {
        if (shouldSkipWs(chars)) {
            skipComments(chars)
        }
        val index = this.index
        if (index >= this.end) {
            error("Unexpected EOF in object key")
        }
        val keyChar = chars[index]
        return when {
            keyChar == '"' || keyChar == '\'' -> parseString(chars)
            keyChar == '+' || keyChar == '-' || keyChar == '.' || keyChar.isDigit() -> parseNumber(chars).toString()
            CasonUtil.isIdStart(keyChar) -> parseIdentifier(chars)
            else -> error("Invalid object key start '$keyChar'")
        }
    }

    protected fun parseArray(chars: CharArray): JSONArray {
        expectChar(chars, '[')
        if (shouldSkipWs(chars)) {
            skipComments(chars)
        }

        val list = ArrayList<JSONElement>(24)

        if (peekChar(chars) == ']') {
            readCharNoLine(chars)
            return JSONArray(list)
        }

        while (true) {
            list.add(parseElement(chars))
            if (shouldSkipWs(chars)) {
                skipComments(chars)
            }

            when (peekChar(chars)) {
                ',' -> {
                    readCharNoLine(chars)
                    if (shouldSkipWs(chars)) {
                        skipComments(chars)
                    }
                }
                ']' -> {
                    readCharNoLine(chars)
                    break
                }
                else -> error("Expected ',' or ']' in array")
            }
        }

        return JSONArray(list)
    }

    open fun shouldSkipWs(chars: CharArray): Boolean {
        val index = this.index
        if (index < this.end) {
            val currentChar = chars[index]
            return currentChar <= ' ' || currentChar == '/'
        }
        return true
    }

    fun skipComments(chars: CharArray) {
        var index = this.index
        val end = this.end
        var line = this.line
        var col = this.col

        while (true) {
            // Skip whitespace and line terminators.
            while (index < end) {
                val currentChar = chars[index]

                // Fast whitespace (space / tab etc.).
                if (CasonUtil.isWs(currentChar)) {
                    index++
                    col++
                    continue
                }

                // Line terminators.
                if (currentChar == '\n' || currentChar == '\u2028' || currentChar == '\u2029') {
                    index++
                    line++
                    col = 1
                    continue
                }
                if (currentChar == '\r') {
                    if (index + 1 < end) {
                        if (chars[index + 1] == '\n') {
                            index += 2
                        } else {
                            index++
                        }
                    } else {
                        if (!this.isFinal) {
                            // Commit reader.
                            this.index = index
                            this.line = line
                            this.col = col

                            throw NeedMoreInputException(this.index, this.line, this.col)
                        }
                        index++
                    }
                    line++
                    col = 1
                    continue
                }
                break
            }

            // Line comment '//' .
            if (index + 1 < end && chars[index] == '/' && chars[index + 1] == '/') {
                index += 2
                col += 2

                while (index < end) {
                    val c = chars[index]
                    if (c == '\n' || c == '\u2028' || c == '\u2029') {
                        break
                    }
                    if (c == '\r') {
                        break
                    }
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
                        // Commit reader.
                        this.index = index
                        this.line = line
                        this.col = col

                        if (this.isFinal) {
                            error("Unterminated block comment")
                        }
                        throw NeedMoreInputException(this.index, this.line, this.col)
                    }
                    val commentChar = chars[index]
                    if (commentChar == '*' && chars[index + 1] == '/') {
                        index += 2
                        col += 2
                        break
                    }
                    if (commentChar == '\n' || commentChar == '\u2028' || commentChar == '\u2029') {
                        index++
                        line++
                        col = 1
                        continue
                    }

                    if (commentChar == '\r') {
                        if (index + 1 < end && chars[index + 1] == '\n') {
                            index += 2
                        } else {
                            index++
                        }
                        line++
                        col = 1
                        continue
                    }

                    index++
                    col++
                }
                continue
            }

            break
        }

        // Commit reader.
        this.index = index
        this.line = line
        this.col = col
    }

    protected open fun parseString(chars: CharArray): String {
        ensureAvailable()
        val quote = chars[this.index]
        this.index++
        this.col++

        val start = this.index
        var index = start
        val end = this.end

        // Fast path: only look for quote or backslash.
        while (true) {
            if (index >= end) {
                if (this.isFinal) {
                    error("Unterminated string")
                }
                throw NeedMoreInputException(this.index, this.line, this.col)
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

        var col = this.col

        while (true) {
            if (index >= end) {
                if (this.isFinal) {
                    error("Unterminated string")
                }
                throw NeedMoreInputException(this.index, this.line, this.col)
            }

            val c = chars[index++]
            col++

            when (c) {
                quote -> {
                    this.index = index
                    this.col = col
                    return builder.toString()
                }

                '\\' -> {
                    if (index >= end) {
                        if (this.isFinal) {
                            error("Unterminated escape in string")
                        }
                        throw NeedMoreInputException(this.index, this.line, this.col)
                    }
                    val esc = chars[index++]
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
                    if (index >= end && !this.isFinal) {
                        throw NeedMoreInputException(this.index, this.line, this.col)
                    }
                    error("Unescaped line terminator in string")
                }

                else -> builder.append(c)
            }
        }
    }

    protected fun parseNumber(chars: CharArray): CasonNumber {
        val end = this.end

        var index = this.index
        var col = this.col

        // Sign.
        var sign = 1
        if (index >= end) {
            if (this.isFinal) {
                error("Invalid number")
            }
            throw NeedMoreInputException(this.index, this.line, this.col)
        }
        val first = chars[index]
        if (first == '+' || first == '-') {
            sign = if (first == '-') {
                -1
            } else {
                1
            }
            index++
            col++
            if (index >= end) {
                if (this.isFinal) {
                    error("Invalid number")
                }
                throw NeedMoreInputException(this.index, this.line, this.col)
            }
        }

        // Mantissa.
        var mantissa = 0L
        var digits = 0
        var exp10 = 0
        var overflow = false

        // integer part
        while (index < end) {
            val c = chars[index]
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
        if (index < end && chars[index] == '.') {
            index++
            col++
            while (index < end) {
                val c = chars[index]
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
        if (index < end) {
            val c = chars[index]
            if (c == 'e' || c == 'E') {
                index++
                col++

                if (index >= end) {
                    if (this.isFinal) {
                        error("Invalid exponent in number")
                    }
                    throw NeedMoreInputException(this.index, this.line, this.col)
                }

                var expSign = 1
                val s = chars[index]
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
                while (index < end) {
                    val dch = chars[index]
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
        val bd: BigDecimal = if (!overflow && digits <= 18) {
            // Fast path: fits in signed Long safely.
            if (exp10 == 0) {
                BigDecimal.valueOf(sign * mantissa)
            } else {
                BigDecimal.valueOf(sign * mantissa).scaleByPowerOfTen(exp10)
            }
        } else {
            // Slow path: fallback to BigInteger.
            var bd = BigDecimal(mantissa)
            if (sign < 0) {
                bd = bd.negate()
            }
            if (exp10 == 0) {
                bd
            } else {
                bd.scaleByPowerOfTen(exp10)
            }
        }

        return CasonNumber.finite(bd)
    }

    protected fun parseIdentifier(chars: CharArray): String {
        val start = this.index
        var index = start + 1
        val end = this.end

        // First char must exist and be idStart (caller checked).
        ensureAvailable()

        while (index < end && CasonUtil.isIdPart(chars[index])) {
            index++
        }

        // Commit index.
        this.col += index - this.index + 1
        this.index = index

        return String(chars, start, index - start)
    }
}

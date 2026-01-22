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

        fun parse(input: String): JSONElement = parse(input.toCharArray())

        fun parse(input: CharArray): JSONElement {
            val parser = JSONParser(0, input.size, true)
            val element = parser.parseElement(input)
            parser.skipWsAndComments(input)
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
        throw JSONParseException("$msg at line ${this.line}, column ${this.col}")
    }

    protected fun ensureAvailable() {
        if (this.index < this.end) {
            return
        }
        if (this.isFinal) {
            error("Unexpected EOF")
        }
        throw NeedMoreInputException(this.index, this.line, this.col)
    }

    fun peekChar(chars: CharArray): Char = chars[this.index]

    protected fun readCharNoLine(chars: CharArray): Char {
        // Fast path: for structural / number / identifier scanning (no line terminators expected).
        ensureAvailable()
        val currentChar = chars[this.index++]
        this.col++
        return currentChar
    }

    protected fun expectChar(chars: CharArray, expected: Char) {
        val gotChar = readCharNoLine(chars)
        if (gotChar != expected) {
            error("Expected '$expected' but got '$gotChar'")
        }
    }

    protected open fun parseElement(chars: CharArray): JSONElement {
        skipWsAndComments(chars)
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
        return when (peekChar(chars)) {
            'n' -> {
                this.index += 4
                JSONNull
            }
            't' -> {
                this.index += 4
                JSONBoolean.TRUE
            }
            'f' -> {
                this.index += 5
                JSONBoolean.FALSE
            }
            'I' -> {
                this.index += 8
                JSONNumber.POSITIVE_INFINITY
            }
            'N' -> {
                this.index += 3
                JSONNumber.NAN
            }
            else -> error("Unexpected identifier '${parseIdentifier(chars)}'")
        }
    }

    protected open fun parseObject(chars: CharArray): JSONObject {
        expectChar(chars, '{')
        skipWsAndComments(chars)

        val index = this.index
        if (chars[index] == '}') {
            this.index++
            this.col++
            return JSONObject()
        }

        val map = LinkedHashMap<String, JSONElement>(24)
        while (true) {
            if (chars[this.index] == '}') {
                this.index++
                this.col++
                break
            }

            val key = parseObjectKey(chars)
            skipWsAndComments(chars)

            // ':' is structural, no line break expected.
            expectChar(chars, ':')
            val value = parseElement(chars)
            map[key] = value
            skipWsAndComments(chars)

            when (peekChar(chars)) {
                ',' -> {
                    this.index++
                    this.col++
                    skipWsAndComments(chars)
                }

                '}' -> {
                    this.index++
                    this.col++
                    break
                }

                else -> error("Expected ',' or '}' in object")
            }
        }

        return JSONObject(map)
    }

    protected open fun parseObjectKey(chars: CharArray): String {
        skipWsAndComments(chars)

        val index = this.index
        if (index < this.end) {
            val keyChar = chars[index]
            return when {
                keyChar == '"' || keyChar == '\'' -> parseString(chars)
                keyChar == '+' || keyChar == '-' || keyChar == '.' || keyChar.isDigit() -> parseNumber(chars).toString()
                CasonUtil.isIdStart(keyChar) -> parseIdentifier(chars)
                else -> error("Invalid object key start '$keyChar'")
            }
        } else {
            error("Unexpected EOF in object key")
        }
    }

    protected fun parseArray(chars: CharArray): JSONArray {
        expectChar(chars, '[')
        skipWsAndComments(chars)

        if (peekChar(chars) == ']') {
            readCharNoLine(chars)
            return JSONArray()
        }

        val list = ArrayList<JSONElement>(24)

        while (true) {
            if (peekChar(chars) == ']') {
                readCharNoLine(chars)
                break
            }

            list.add(parseElement(chars))

            skipWsAndComments(chars)

            when (peekChar(chars)) {
                ',' -> {
                    readCharNoLine(chars)
                    skipWsAndComments(chars)
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

    fun skipWsAndComments(chars: CharArray) {
        val index = this.index
        if (index >= this.end) {
            return
        }
        val currentChar = chars[index]
        if (currentChar > ' ' && currentChar != '/') {
            return
        }
        skipComments(chars, index)
    }

    fun skipComments(chars: CharArray, inoutIndex: Int) {
        var index = inoutIndex
        val end = this.end
        var line = this.line
        var col = this.col

        while (true) {
            // Skip whitespace and line terminators.
            while (index < end) {
                val currentChar = chars[index]

                // Fast whitespace (space / tab etc.).
                if (currentChar == ' ' || currentChar == '\t') {
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

        val start = this.index
        val end = this.end
        var index = start

        val quote = chars[index++]
        this.col++


        // Fast path: only look for quote or backslash.
        while (true) {
            if (index < end) {
                val currentChar = chars[index]
                if (currentChar == quote) {
                    this.index = index + 1
                    this.col += (index - start) + 1
                    return String(chars, start, index - start)
                }
                if (currentChar == '\\') {
                    break
                }
                index++
                continue
            }
            if (this.isFinal) {
                error("Unterminated string")
            }
            throw NeedMoreInputException(this.index, this.line, this.col)
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
        if (index < end) {
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
            val decimal: BigDecimal = if (overflow || digits > 18) {
                // Slow path: fallback to BigInteger.
                var decimal = BigDecimal.valueOf(mantissa)
                if (sign < 0) {
                    decimal = decimal.negate()
                }
                if (exp10 == 0) {
                    decimal
                } else {
                    decimal.scaleByPowerOfTen(exp10)
                }
            } else {
                // Fast path: fits in signed Long safely.
                if (exp10 == 0) {
                    BigDecimal.valueOf(sign * mantissa)
                } else {
                    BigDecimal.valueOf(sign * mantissa).scaleByPowerOfTen(exp10)
                }
            }

            return CasonNumber.finite(decimal)
        }
        if (this.isFinal) {
            error("Invalid number")
        }
        throw NeedMoreInputException(this.index, this.line, this.col)
    }

    protected fun parseIdentifier(chars: CharArray): String {
        val start = this.index
        val end = this.end
        var index = start + 1

        while (index < end && CasonUtil.isIdPart(chars[index])) {
            index++
        }

        this.index = index
        this.col += index - start

        return String(chars, start, index - start)
    }
}

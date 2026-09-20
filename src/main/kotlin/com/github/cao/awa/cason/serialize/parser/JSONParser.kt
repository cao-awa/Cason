@file:Suppress("unused")

package com.github.cao.awa.cason.serialize.parser

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.array.JSONArray
import com.github.cao.awa.cason.exception.JSONParseException
import com.github.cao.awa.cason.exception.NeedMoreInputException
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.primary.JSONBoolean
import com.github.cao.awa.cason.primary.JSONNull
import com.github.cao.awa.cason.primary.JSONNumber
import com.github.cao.awa.cason.primary.JSONString
import com.github.cao.awa.cason.util.CasonUtil
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.pow

open class JSONParser {
    companion object {
        private val DIGIT = BooleanArray(128)

        init {
            for (c in '0'..'9') {
                DIGIT[c.code] = true
            }
        }

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
        throw JSONParseException("$msg, at line ${this.line}, column ${this.col}")
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
            '+', '-', '.', in '0'..'9' -> parseNumber(chars)
            else -> parseIdentifierValueOrError(chars, firstChar)
        }
    }

    protected fun parseIdentifierValueOrError(chars: CharArray, c: Char): JSONElement {
        return when (peekChar(chars)) {
            'n' -> {
                this.index += 4
                this.col += 4
                JSONNull
            }

            't' -> {
                this.index += 4
                this.col += 4
                JSONBoolean.TRUE
            }

            'f' -> {
                this.index += 5
                this.col += 5
                JSONBoolean.FALSE
            }

            'I' -> {
                this.index += 8
                JSONNumber.POSITIVE_INFINITY
            }

            'N' -> {
                this.index += 3
                this.col += 3
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

            this.col += key.length

            // ':' is structural, no line break expected.
            expectChar(chars, ':')
            this.col++
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
            val result = when {
                keyChar == '"' || keyChar == '\'' -> {
                    parseString(chars)
                }

                keyChar == '+' || keyChar == '-' || keyChar == '.' || keyChar.isDigit() -> parseNumber(chars).toString()
                CasonUtil.isIdStart(keyChar) -> parseIdentifier(chars)
                else -> error("Invalid object key start '$keyChar'")
            }
            this.col += result.length
            return result
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
                        col = 0
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
                    return String(chars, start + 1, index - start - 1)
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

    /**
     * Parse a JSON number.
     *
     * Supported forms:
     *   123
     *   +123
     *   -123
     *   123.45
     *   .45
     *   123.
     *   1e10
     *   1.5e-10
     *   +1.5E+10
     *
     * The parser first scans the complete number token and then chooses
     * an appropriate representation:
     *
     *   Int -> Long -> Double -> BigDecimal
     *
     * BigDecimal is used as the final lossless fallback.
     */
    protected fun parseNumber(input: CharArray): JSONNumber {
        val start = this.index
        var i = start

        if (i >= this.end) {
            ensureAvailable()
        }

        // Sign.
        if (input[i] == '+' || input[i] == '-') {
            i++

            if (i >= this.end) {
                if (!this.isFinal) {
                    throw NeedMoreInputException(i, this.line, this.col)
                }
                error("Incomplete number")
            }
        }

        // Integer part.
        //
        // Intentionally allow:
        //   123
        //   .123
        //
        // and also preserve the existing parser's support for:
        //   123.
        var integerDigits = 0

        while (i < this.end) {
            val c = input[i]
            if (c in '0'..'9') {
                integerDigits++
                i++
            } else {
                break
            }
        }

        // Fraction.
        var hasFraction = false
        var fractionDigits = 0

        if (i < this.end && input[i] == '.') {
            hasFraction = true
            i++

            while (i < this.end) {
                val c = input[i]
                if (c in '0'..'9') {
                    fractionDigits++
                    i++
                } else {
                    break
                }
            }

            // Keep compatibility with the original parser:
            // "1." is accepted.
            //
            // But "." alone is not a valid number.
            if (integerDigits == 0 && fractionDigits == 0) {
                error("Invalid number")
            }
        }

        // "." without an integer part and without fraction digits
        // has already been rejected above.
        if (integerDigits == 0 && fractionDigits == 0) {
            error("Invalid number")
        }

        // Exponent.
        var hasExponent = false
        var exponentDigits = 0

        if (i < this.end && (input[i] == 'e' || input[i] == 'E')) {
            hasExponent = true
            i++

            if (i >= this.end) {
                if (!this.isFinal) {
                    throw NeedMoreInputException(i, this.line, this.col)
                }
                error("Incomplete exponent")
            }

            if (input[i] == '+' || input[i] == '-') {
                i++

                if (i >= this.end) {
                    if (!this.isFinal) {
                        throw NeedMoreInputException(i, this.line, this.col)
                    }
                    error("Incomplete exponent")
                }
            }

            while (i < this.end) {
                val c = input[i]
                if (c in '0'..'9') {
                    exponentDigits++
                    i++
                } else {
                    break
                }
            }

            if (exponentDigits == 0) {
                error("Expected exponent digits")
            }
        }

        // If the number reaches the end of a non-final input buffer,
        // we cannot know whether the number continues in the next chunk.
        //
        // Example:
        //   "123" + next chunk "456"
        //
        // Therefore streaming callers must retry when the token ends
        // exactly at the current buffer boundary.
        if (i >= this.end && !this.isFinal) {
            throw NeedMoreInputException(i, this.line, this.col)
        }

        // Number must be followed by a legal delimiter.
        //
        // Without this check:
        //   123abc
        //
        // could incorrectly become two unrelated tokens.
        if (i < this.end && !isNumberDelimiter(input[i])) {
            error("Invalid character '${input[i]}' after number")
        }

        val tokenLength = i - start
        val token = String(input, start, tokenLength)

        // Commit parser position exactly once.
        this.index = i
        this.col += tokenLength

        // Integer
        if (!hasFraction && !hasExponent) {
            // Avoid Long overflow entirely by checking the token through
            // BigInteger only when necessary.
            //
            // Small integers remain allocation-free in the common case.
            if (tokenLength <= 10) {
                val value = token.toLong()

                if (value in Int.MIN_VALUE..Int.MAX_VALUE) {
                    return JSONNumber.ofInt(value.toInt())
                }

                return JSONNumber.ofLong(value)
            }

            if (tokenLength <= 19) {
                val value = token.toLongOrNull()

                if (value != null) {
                    if (value in Int.MIN_VALUE..Int.MAX_VALUE) {
                        return JSONNumber.ofInt(value.toInt())
                    }

                    return JSONNumber.ofLong(value)
                }
            }

            // Too large for Long.
            return JSONNumber.ofBig(BigDecimal(token))
        }

        // Decimal / exponent
        //
        // Keep the existing behavior of using Double for reasonably
        // small finite numbers, but fall back to BigDecimal when Double
        // overflows or the representation is too large.
        if (tokenLength <= 20) {
            val value = token.toDoubleOrNull()

            if (value != null && value.isFinite()) {
                return JSONNumber.ofDouble(value)
            }
        }

        // Exact fallback.
        return JSONNumber.ofBig(BigDecimal(token))
    }


    /**
     * Characters which can legally terminate a number.
     *
     * Whitespace/comments are handled by the caller, so they are valid
     * delimiters here.
     */
    private fun isNumberDelimiter(c: Char): Boolean {
        return when (c) {
            ',', ']', '}', ':',
            ' ', '\t', '\n', '\r', '\u2028', '\u2029',
            '/' -> true

            else -> false
        }
    }


    /**
     * Parse the current number as BigDecimal.
     *
     * Unlike the old implementation, this supports:
     *
     *   123
     *   -123
     *   1.23
     *   .23
     *   1e10
     *   1.23e-10
     *   +1.23E+10
     *
     * The parser position is advanced to the character immediately
     * following the number.
     */
    fun parseBigDecimal(input: CharArray): BigDecimal {
        val start = this.index
        var i = start

        if (i >= this.end) {
            if (!this.isFinal) {
                throw NeedMoreInputException(i, this.line, this.col)
            }
            throw NumberFormatException("Empty input")
        }

        // Sign
        if (input[i] == '+' || input[i] == '-') {
            i++

            if (i >= this.end) {
                if (!this.isFinal) {
                    throw NeedMoreInputException(i, this.line, this.col)
                }
                throw NumberFormatException("Incomplete number")
            }
        }

        var integerDigits = 0

        while (i < this.end && input[i] in '0'..'9') {
            integerDigits++
            i++
        }

        var fractionDigits = 0

        if (i < this.end && input[i] == '.') {
            i++

            while (i < this.end && input[i] in '0'..'9') {
                fractionDigits++
                i++
            }
        }

        if (integerDigits == 0 && fractionDigits == 0) {
            throw NumberFormatException("Invalid number")
        }

        // Exponent
        if (i < this.end && (input[i] == 'e' || input[i] == 'E')) {
            i++

            if (i >= this.end) {
                if (!this.isFinal) {
                    throw NeedMoreInputException(i, this.line, this.col)
                }
                throw NumberFormatException("Incomplete exponent")
            }

            if (input[i] == '+' || input[i] == '-') {
                i++
            }

            val exponentStart = i

            while (i < this.end && input[i] in '0'..'9') {
                i++
            }

            if (i == exponentStart) {
                throw NumberFormatException("Expected exponent digits")
            }
        }

        if (i >= this.end && !this.isFinal) {
            throw NeedMoreInputException(i, this.line, this.col)
        }

        if (i < this.end && !isNumberDelimiter(input[i])) {
            throw NumberFormatException(
                "Invalid character '${input[i]}' after number"
            )
        }

        val length = i - start
        val token = String(input, start, length)

        this.index = i
        this.col += length

        return BigDecimal(token)
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

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

    fun CharReader.parseElement(): JSONElement {
        skipWsAndCommentsFast()
        val c = peek() ?: run {
            if (this.isFinal) {
                error("Expected a value but got EOF")
            }
            throw NeedMoreInputException(this)
        }
        return when (c) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"', '\'' -> JSONString(parseStringFast())
            '+', '-', '.', in '0'..'9' -> JSONNumber(parseNumberFast())
            else -> parseIdentifierValueOrError(c)
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
        expect('{')
        skipWsAndCommentsFast()
        val map = LinkedHashMap<String, JSONElement>(16)

        if (peek() == '}') {
            next()
            return JSONObject(map)
        }

        while (true) {
            val key = parseObjectKey()
            skipWsAndCommentsFast()
            expect(':')
            val value = parseElement()
            map[key] = value
            skipWsAndCommentsFast()

            when (peek()) {
                ',' -> {
                    next(); skipWsAndCommentsFast()
                }

                '}' -> {
                    next(); break
                }

                else -> error("Expected ',' or '}' in object")
            }
        }
        return JSONObject(map)
    }

    private fun CharReader.parseObjectKey(): String {
        skipWsAndCommentsFast()
        val c = peek() ?: error("Unexpected EOF in object key")
        return when {
            c == '"' || c == '\'' -> parseStringFast()
            c == '+' || c == '-' || c == '.' || c.isDigit() -> parseNumberFast().toString()
            CasonUtil.isIdStart(c) -> parseIdentifierFast()
            else -> error("Invalid object key start '$c'")
        }
    }

    private fun CharReader.parseArray(): JSONArray {
        expect('[')
        skipWsAndCommentsFast()
        val list = ArrayList<JSONElement>(16)

        if (peek() == ']') {
            next()
            return JSONArray(list)
        }

        while (true) {
            list.add(parseElement())
            skipWsAndCommentsFast()
            when (peek()) {
                ',' -> {
                    next()
                    skipWsAndCommentsFast()
                }

                ']' -> {
                    next()
                    break
                }

                else -> error("Expected ',' or ']' in array")
            }
        }
        return JSONArray(list)
    }

    fun CharReader.skipWsAndCommentsFast() {
        val chars = this.chars
        var i = this.index
        val end = this.end

        while (true) {
            // Whitespace.
            while (i < end && (CasonUtil.isWs(chars[i]) || CasonUtil.isLineTerminator(chars[i]))) {
                i++
            }

            // Comment '//'.
            if (i + 1 < end && chars[i] == '/' && chars[i + 1] == '/') {
                i += 2
                while (i < end && !CasonUtil.isLineTerminator(chars[i])) {
                    i++
                }
                continue
            }

            // Comment '/*'.
            if (i + 1 < end && chars[i] == '/' && chars[i + 1] == '*') {
                i += 2
                while (true) {
                    if (i + 1 >= end) {
                        if (this.isFinal) {
                            error("Unterminated block comment")
                        }
                        throw NeedMoreInputException(this)
                    }
                    if (chars[i] == '*' && chars[i + 1] == '/') {
                        i += 2
                        break
                    }
                    i++
                }
                continue
            }

            break
        }

        this.col += (i - this.index)
        this.index = i
    }

    private fun CharReader.parseStringFast(): String {
        val quote = next()!!
        val chars = this.chars
        val start = this.index
        var i = start

        while (true) {
            if (i >= this.end) {
                if (this.isFinal) error("Unterminated string")
                throw NeedMoreInputException(this)
            }
            val c = chars[i]
            if (c == quote) {
                this.index = i + 1
                this.col += (this.index - start) + 1
                return String(chars, start, i - start)
            }
            if (c == '\\') {
                break
            }
            if (c == '\n' || c == '\r') {
                error("Unescaped line terminator in string")
            }
            i++
        }

        val sb = StringBuilder(i - start + 16)
        sb.appendRange(chars, start, start + (i - start))
        this.index = i

        while (true) {
            val c = next() ?: error("Unterminated string")
            when (c) {
                quote -> return sb.toString()
                '\\' -> {
                    val e = next() ?: error("Unterminated escape")
                    sb.append(
                        when (e) {
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            'b' -> '\b'
                            'f' -> '\u000C'
                            '"' -> '"'
                            '\'' -> '\''
                            '\\' -> '\\'
                            else -> error("Unknown escape \\$e")
                        }
                    )
                }

                else -> sb.append(c)
            }
        }
    }

    private fun CharReader.parseNumberFast(): CasonNumber {
        var sign = 1
        val c = peek()!!
        if (c == '+' || c == '-') {
            sign = if (c == '-') -1 else 1
            next()
        }

        var mantissa = 0L
        var mantissaDigits = 0
        var exp10 = 0
        var overflow = false

        fun addDigit(d: Int) {
            if (!overflow) {
                val n = mantissa * 10 + d
                if (n < mantissa) {
                    overflow = true
                } else {
                    mantissa = n
                }
            }
            mantissaDigits++
        }

        while (peek()?.isDigit() == true) {
            addDigit(next()!! - '0')
        }

        if (peek() == '.') {
            next()
            while (peek()?.isDigit() == true) {
                addDigit(next()!! - '0')
                exp10--
            }
        }

        if (mantissaDigits == 0) error("Invalid number")

        if (peek() == 'e' || peek() == 'E') {
            next()
            var expSign = 1
            if (peek() == '+' || peek() == '-') {
                expSign = if (next() == '-') -1 else 1
            }
            var exp = 0
            while (peek()?.isDigit() == true) {
                exp = exp * 10 + (next()!! - '0')
            }
            exp10 += expSign * exp
        }

        val bd = if (!overflow && mantissaDigits <= 18) {
            BigDecimal.valueOf(sign * mantissa).scaleByPowerOfTen(exp10)
        } else {
            var bi = BigInteger.valueOf(mantissa)
            bi = if (sign < 0) {
                bi.negate()
            } else {
                bi
            }
            BigDecimal(bi).scaleByPowerOfTen(exp10)
        }

        return CasonNumber.finite(bd)
    }

    private fun CharReader.parseIdentifierFast(): String {
        val chars = this.chars
        val start = this.index
        next()
        while (this.index < this.end && CasonUtil.isIdPart(chars[this.index])) {
            next()
        }
        return String(chars, start, this.index - start)
    }
}

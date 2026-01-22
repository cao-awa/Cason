@file:Suppress("unused")

package com.github.cao.awa.cason.serialize.parser

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.array.JSONArray
import com.github.cao.awa.cason.exception.NeedMoreInputException
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.primary.JSONBoolean
import com.github.cao.awa.cason.primary.JSONNull
import com.github.cao.awa.cason.primary.JSONNumber
import com.github.cao.awa.cason.primary.JSONString

class StrictJSONParser(
    start: Int,
    end: Int,
    isFinal: Boolean
) : JSONParser(start, end, isFinal) {
    companion object {
        fun parseObject(input: String): JSONObject =
            parse(input) as JSONObject

        fun parseObject(input: CharArray): JSONObject =
            parse(input) as JSONObject

        fun parseArray(input: String): JSONArray =
            parse(input) as JSONArray

        fun parseArray(input: CharArray): JSONArray =
            parse(input) as JSONArray

        fun parse(input: String): JSONElement = parse(input.toCharArray())

        fun parse(input: CharArray): JSONElement {
            val parser = StrictJSONParser(0, input.size, true)
            val element = parser.parseElement(input)

            parser.skipWhitespaceIfAny(input)

            if (parser.eof()) {
                return element
            }
            parser.error("Trailing characters after top-level value")
        }
    }

    override fun parseElement(chars: CharArray): JSONElement {
        skipWhitespaceIfAny(chars)
        ensureAvailable()

        return when (val currentChar = chars[this.index]) {
            '"' -> JSONString(parseString(chars))
            '{' -> parseObject(chars)
            '[' -> parseArray(chars)
            '-', in '0'..'9' -> JSONNumber(parseNumber(chars))
            'n', 't', 'f' -> parseLiteral(chars)
            else -> error("Unexpected character '$currentChar'")
        }
    }

    private fun parseLiteral(chars: CharArray): JSONElement {
        val currentChar = peekChar(chars)
        return when (currentChar) {
            'n' -> {
                this.col += 4
                this.index += 4
                JSONNull
            }
            't' -> {
                this.col += 4
                this.index += 4
                JSONBoolean.TRUE
            }
            'f' -> {
                this.col += 5
                this.index += 5
                JSONBoolean.FALSE
            }
            else -> error("Unexpected identifier '${parseIdentifier(chars)}'")
        }
    }

    override fun parseObject(chars: CharArray): JSONObject {
        expectChar(chars, '{')
        skipWhitespaceIfAny(chars)

        val index = this.index
        if (chars[index] == '}') {
            readCharNoLine(chars)
            return JSONObject()
        }

        val end = this.end
        val map = HashMap<String, JSONElement>(24)
        while (true) {
            // Object key (must be string).
            skipWhitespaceIfAny(chars)
            if (index < end) {
                val key = parseString(chars)

                skipWhitespaceIfAny(chars)
                expectChar(chars, ':')

                map[key] = parseElement(chars)
                skipWhitespaceIfAny(chars)

                when (peekChar(chars)) {
                    ',' -> {
                        readCharNoLine(chars)
                        skipWhitespaceIfAny(chars)
                    }

                    '}' -> {
                        readCharNoLine(chars)
                        break
                    }

                    else -> error("Expected ',' or '}' in object")
                }
            } else {
                error("Invalid object key start '${peekChar(chars)}'")
            }
        }

        return JSONObject(map)
    }

    private fun skipWhitespaceIfAny(chars: CharArray) {
        val index = this.index
        if (index < this.end && chars[index] <= ' ') {
            skipWhitespace(chars)
        }
    }

    private fun skipWhitespace(chars: CharArray) {
        var index = this.index
        var line = this.line
        var col = this.col
        val end = this.end

        while (index < end) {
            when (chars[index]) {
                ' ', '\t' -> {
                    index++
                    col++
                }
                '\n' -> {
                    index++
                    line++
                    col = 1
                }
                '\r' -> {
                    if (index + 1 < end && chars[index + 1] == '\n') {
                        index += 2
                    } else {
                        index++
                    }
                    line++
                    col = 1
                }
                else -> break
            }
        }

        this.index = index
        this.line = line
        this.col = col
    }

    override fun parseString(chars: CharArray): String {
        ensureAvailable()
        val start = this.index
        var index = start
        val end = this.end
        var col = this.col

        if (chars[index] == '"') {
            index++
            col++

            // F ast path: no escape.
            while (index < end) {
                val c = chars[index]
                if (c == '"') {
                    this.index = index + 1
                    this.col += (index - start) + 1
                    return String(chars, start, index - start)
                }
                if (c == '\\') {
                    break
                }
                index++
            }

            if (index >= end) {
                if (this.isFinal) {
                    error("Unterminated string")
                }
                throw NeedMoreInputException(this.index, this.line, this.col)
            }

            // Slow path: with escape.
            val builder = StringBuilder(index - start)
            builder.appendRange(chars, start, index)

            this.index = index
            this.col += index - start

            while (true) {
                if (this.index >= end) {
                    if (this.isFinal) {
                        error("Unterminated string")
                    }

                    throw NeedMoreInputException(this.index, this.line, this.col)
                }

                val currentChar = chars[this.index++]
                col++

                when (currentChar) {
                    '"' -> return builder.toString()
                    '\\' -> {
                        if (this.index >= end) {
                            if (this.isFinal) {
                                error("Unterminated escape in string")
                            }
                            this.col = col
                            throw NeedMoreInputException(index, this.line, col)
                        }
                        val esc = chars[index++]
                        col++
                        builder.append(
                            when (esc) {
                                '"', '\\' -> esc
                                'n' -> '\n'
                                'r' -> '\r'
                                't' -> '\t'
                                'b' -> '\b'
                                'f' -> '\u000C'
                                else -> error("Unknown escape \\$esc")
                            }
                        )
                    }

                    '\n', '\r' -> error("Unescaped line terminator in string")
                    else -> builder.append(currentChar)
                }
            }
        } else {
            error("Expected '\"' to start string")
        }
    }
}

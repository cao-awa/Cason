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

class StrictJSONParser: JSONParser {
    companion object {
        fun parseObject(input: String): JSONObject = parse(input) as JSONObject
        fun parseArray(input: String): JSONArray = parse(input) as JSONArray

        fun parse(input: String): JSONElement {
            val chars = input.toCharArray()
            val parser = StrictJSONParser(0, chars.size, true)
            val element = parser.parseElement(chars)

            if (parser.shouldSkipWs(chars)) {
                parser.skipWhitespace(chars)
            }

            if (parser.eof()) {
                return element
            } else {
                parser.error("Trailing characters after top-level value")
            }
        }
    }

    constructor(start: Int, end: Int, isFinal: Boolean) : super(start, end, isFinal)

    override fun parseElement(chars: CharArray): JSONElement {
        if (shouldSkipWs(chars)) {
            skipWhitespace(chars)
        }
        ensureAvailable()
        return when (val c = chars[this.index]) {
            '{' -> parseObject(chars)
            '[' -> parseArray(chars)
            '"' -> JSONString(parseString(chars))
            '-', in '0'..'9' -> JSONNumber(parseNumber(chars))
            else -> parseLiteralOrError(chars, c)
        }
    }

    private fun parseLiteralOrError(chars: CharArray, c: Char): JSONElement {
        return when (c) {
            'n', 't', 'f' -> {
                when (val id = parseIdentifier(chars)) {
                    "null" -> JSONNull
                    "true" -> JSONBoolean(true)
                    "false" -> JSONBoolean(false)
                    else -> error("Unexpected identifier '$id'")
                }
            }
            else -> error("Unexpected character '$c'")
        }
    }

    override fun parseObject(chars: CharArray): JSONObject {
        expectChar(chars, '{')
        if (shouldSkipWs(chars)) {
            skipWhitespace(chars)
        }

        val map = LinkedHashMap<String, JSONElement>(24)

        if (peekChar(chars) == '}') {
            readCharNoLine(chars)
            return JSONObject(map)
        }

        while (true) {
            val key = parseObjectKey(chars)
            if (shouldSkipWs(chars)) {
                skipWhitespace(chars)
            }

            expectChar(chars, ':')
            map[key] = parseElement(chars)

            if (shouldSkipWs(chars)) {
                skipWhitespace(chars)
            }

            when (peekChar(chars)) {
                ',' -> {
                    readCharNoLine(chars)
                    if (shouldSkipWs(chars)) {
                        skipWhitespace(chars)
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

    override fun parseObjectKey(chars: CharArray): String {
        if (shouldSkipWs(chars)) {
            skipWhitespace(chars)
        }
        val index = this.index
        if (index >= this.end) {
            error("Unexpected EOF in object key")
        }
        if (chars[index] == '"') {
            return parseString(chars)
        }
        error("Invalid object key start '${chars[index]}' (expected '\"')")
    }

    override fun shouldSkipWs(chars: CharArray): Boolean {
        val index = this.index
        return index < this.end && chars[index] <= ' '
    }

    fun skipWhitespace(chars: CharArray) {
        var index = this.index
        val end = this.end
        var line = this.line
        var col = this.col

        while (index < end) {
            val currentChar = chars[index]

            if (currentChar == ' ' || currentChar == '\t') {
                index++
                col++
                continue
            }

            if (currentChar == '\n') {
                index++
                line++
                col = 1
                continue
            }

            if (currentChar == '\r') {
                if (index + 1 < end && chars[index + 1] == '\n') {
                    index += 2
                } else {
                    index++
                }
                line++
                col = 1
                continue
            }

            break
        }

        this.index = index
        this.line = line
        this.col = col
    }

    override fun parseString(chars: CharArray): String {
        ensureAvailable()
        if (chars[this.index] != '"') {
            error("Expected '\"' to start string")
        }
        this.index++
        this.col++

        val start = this.index
        var index = start
        val end = this.end

        while (true) {
            if (index < end) {
                val currentChar = chars[index]
                if (currentChar == '"') {
                    this.index = index + 1
                    this.col += (index - start) + 1
                    return String(chars, start, index - start)
                }
                if (currentChar == '\\') {
                    break
                }
                index++
            } else {
                if (this.isFinal) {
                    error("Unterminated string")
                }
                throw NeedMoreInputException(this.index, this.line, this.col)
            }
        }

        val builder = StringBuilder((index - start) + 16)
        builder.appendRange(chars, start, index)

        this.col += index - start
        this.index = index

        var col = this.col

        while (true) {
            if (index < end) {
                val currentChar = chars[index++]
                col++

                when (currentChar) {
                    '"' -> {
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
                                '\\' -> '\\'
                                else -> error("Unknown escape \\$esc")
                            }
                        )
                    }

                    '\n' ->
                        error("Unescaped line terminator in string")

                    '\r' -> {
                        if (end <= index && !this.isFinal) {
                            throw NeedMoreInputException(this.index, this.line, this.col)
                        }
                        error("Unescaped line terminator in string")
                    }

                    else -> builder.append(currentChar)
                }
            } else {
                if (this.isFinal) {
                    error("Unterminated string")
                }
                throw NeedMoreInputException(this.index, this.line, this.col)
            }
        }
    }
}

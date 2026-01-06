package com.github.cao.awa.caoson.parser

import com.github.cao.awa.caoson.JSONElement
import com.github.cao.awa.caoson.arr.JSONArray
import com.github.cao.awa.caoson.exception.JSONParseException
import com.github.cao.awa.caoson.number.CasonNumber
import com.github.cao.awa.caoson.obj.JSONObject
import com.github.cao.awa.caoson.primary.JSONBoolean
import com.github.cao.awa.caoson.primary.JSONNull
import com.github.cao.awa.caoson.primary.JSONNumber
import com.github.cao.awa.caoson.primary.JSONString
import com.github.cao.awa.caoson.setting.JSONSettings
import com.github.cao.awa.caoson.reader.CharReader
import com.github.cao.awa.caoson.serialize.JSONSerializeVersion
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.text.iterator

object JSONParser {
    fun parse(input: String): JSONElement {
        val reader = CharReader(input)
        val element = reader.parseElement()
        reader.skipWsAndComments()
        if (reader.eof()) {
            return element
        }
        reader.error("Trailing characters after top-level value")
    }

    fun stringify(
        value: JSONElement,
        pretty: Boolean = false,
        indent: String = "  "
    ): String {
        val sb = StringBuilder()
        writeValue(sb, value, pretty, indent, 0)
        return sb.toString()
    }

    private fun CharReader.error(msg: String): Nothing {
        val start = maxOf(0, this.index - 20)
        val end = minOf(this.string.length, this.index + 20)
        val excerpt = this.string.substring(start, end).replace("\n", "\\n")
        throw JSONParseException("JSON5 Parse Error @ line ${this.line}, col ${this.col}, index ${this.index}: $msg. Around: \"$excerpt\"")
    }

    private fun CharReader.parseElement(): JSONElement {
        skipWsAndComments()
        val c = peek() ?: error("Expected a value but got EOF")

        return when (c) {
            '{' -> parseObject()
            '[' -> parseArray()
            '\'', '"' -> JSONString(parseString())
            '+', '-', '.', in '0'..'9' -> JSONNumber(parseNumber())
            else -> {
                if (isIdStart(c)) {
                    when (val id = parseIdentifier()) {
                        "null" -> JSONNull
                        "true" -> JSONBoolean(true)
                        "false" -> JSONBoolean(false)
                        "Infinity" -> JSONNumber(CasonNumber.posInf())
                        "NaN" -> JSONNumber(CasonNumber.nan())
                        else -> error("Unexpected identifier '$id' (JSON5 only allows identifiers as object keys, not as bare values)")
                    }
                } else {
                    error("Unexpected character '$c'")
                }
            }
        }
    }

    private fun CharReader.parseObject(): JSONObject {
        expect('{')
        skipWsAndComments()
        val map = LinkedHashMap<String, JSONElement>()
        if (peek() == '}') {
            next()
            return JSONObject(map)
        }

        while (true) {
            skipWsAndComments()
            val key = parseObjectKey()
            skipWsAndComments()
            expect(':')
            val element = parseElement()
            map[key] = element
            skipWsAndComments()

            when (peek()) {
                ',' -> {
                    next()
                    skipWsAndComments()
                    // trailing comma allowed.
                    if (peek() == '}') {
                        next()
                        break
                    }
                }

                '}' -> {
                    next()
                    break
                }

                else -> error("Expected ',' or '}' in object")
            }
        }
        return JSONObject(map)
    }

    private fun CharReader.parseObjectKey(): String {
        skipWsAndComments()
        val c = peek() ?: error("Expected object key but got EOF")
        return if (c == '\'' || c == '"') {
            parseString()
        } else if (c == '+' || c == '-' || c == '.' || c in '0'..'9') {
            // Numeric key literal like {1: "a"} is valid in JS object literal style.
            parseNumber().toString()
        } else {
            if (isIdStart(c)) {
                // In key position, allow keywords too.
                when (val id = parseIdentifier()) {
                    "null", "true", "false", "Infinity", "NaN" -> id
                    else -> id
                }
            } else {
                error("Invalid object key start '$c'")
            }
        }
    }

    private fun CharReader.parseArray(): JSONArray {
        expect('[')
        skipWsAndComments()
        val list = ArrayList<JSONElement>()
        if (peek() == ']') {
            next(); return JSONArray(list)
        }

        while (true) {
            val v = parseElement()
            list.add(v)
            skipWsAndComments()
            when (peek()) {
                ',' -> {
                    next()
                    skipWsAndComments()
                    // trailing comma allowed
                    if (peek() == ']') {
                        next(); break
                    }
                }

                ']' -> {
                    next(); break
                }

                else -> error("Expected ',' or ']' in array")
            }
        }
        return JSONArray(list)
    }

    private fun CharReader.parseString(): String {
        val quote = next() ?: error("Expected quote but got EOF")
        if (quote != '"' && quote != '\'') {
            error("Internal: parseString called at non-quote")
        }

        val builder = StringBuilder()
        while (true) {
            val c = next() ?: error("Unterminated string")
            if (c == quote) {
                return builder.toString()
            }

            if (c == '\\') {
                val n = peek() ?: error("Unterminated escape")
                // Line continuation: backslash + line terminator => skip both
                if (isLineTerminator(n)) {
                    consumeLineTerminator()
                    continue
                }
                val esc = next() ?: error("Unterminated escape")
                when (esc) {
                    '\\' -> builder.append('\\')
                    '/' -> builder.append('/')
                    '\'' -> builder.append('\'')
                    '"' -> builder.append('"')
                    'b' -> builder.append('\b')
                    'f' -> builder.append('\u000C')
                    'n' -> builder.append('\n')
                    'r' -> builder.append('\r')
                    't' -> builder.append('\t')
                    'v' -> builder.append('\u000B')
                    '0' -> {
                        // JSON5: \0 allowed if not followed by digit
                        val p = peek()
                        if (p != null && p.isDigit()) {
                            error("Invalid escape \\0 followed by digit")
                        }
                        builder.append('\u0000')
                    }
                    'x' -> {
                        val h1 = next() ?: error("Invalid \\x escape")
                        val h2 = next() ?: error("Invalid \\x escape")
                        val code = hexVal(h1) * 16 + hexVal(h2)
                        if (code < 0) {
                            error("Invalid hex in \\x escape")
                        }
                        builder.append(code.toChar())
                    }

                    'u' -> {
                        if (peek() == '{') {
                            next() // consume {
                            val hex = StringBuilder()
                            while (true) {
                                val ch = next() ?: error("Unterminated \\u{...} escape")
                                if (ch == '}') {
                                    break
                                }
                                if (!ch.isHexDigit()) {
                                    error("Invalid hex digit in \\u{...}: '$ch'")
                                }
                                hex.append(ch)
                            }
                            if (hex.isEmpty()) {
                                error("Empty \\u{...} escape")
                            }
                            val cp = hex.toString().toInt(16)
                            builder.appendCodePoint(cp)
                        } else {
                            val h = CharArray(4) {
                                next() ?: error("Invalid \\u escape")
                            }
                            val code = h.fold(0) { acc, ch ->
                                val v = hexVal(ch)
                                if (v < 0) {
                                    error("Invalid hex digit in \\u escape: '$ch'")
                                }
                                (acc shl 4) + v
                            }
                            builder.append(code.toChar())
                        }
                    }

                    else -> {
                        // JSON5 allows escaping of line separators? We'll treat unknown escapes as error.
                        error("Unknown escape sequence: \\$esc")
                    }
                }
            } else {
                if (isLineTerminator(c)) error("Unescaped line terminator in string")
                builder.append(c)
            }
        }
    }

    private fun CharReader.parseNumber(): CasonNumber {
        skipWsAndComments()
        val startIdx = index

        // optional sign.
        var sign: Char? = null
        if (peek() == '+' || peek() == '-') sign = next()

        // Infinity / NaN (with optional sign).
        if (peekStartsWith("Infinity")) {
            consumeLiteral("Infinity")
            return if (sign == '-') {
                CasonNumber.negInf()
            } else {
                CasonNumber.posInf()
            }
        }
        if (peekStartsWith("NaN")) {
            consumeLiteral("NaN")
            return CasonNumber.nan() // sign on NaN is allowed in JS, but semantically it's still NaN.
        }

        // Hex: 0x...
        if (peek() == '0' && (peek2() == 'x' || peek2() == 'X')) {
            nextTwice() // consume 0x
            val hex = StringBuilder()
            while (true) {
                val ch = peek() ?: break
                if (!ch.isHexDigit()) {
                    break
                }
                hex.append(next())
            }
            if (hex.isEmpty()) {
                error("Invalid hex literal (no digits)")
            }
            val bi = BigInteger(hex.toString(), 16)
            val bd = BigDecimal(bi)
            return if (sign == '-') {
                CasonNumber.finite(bd.negate())
            } else {
                CasonNumber.finite(bd)
            }
        }

        // Decimal / float / exponent .
        val raw = StringBuilder()
        if (sign != null) {
            raw.append(sign)
        }

        var sawDigit = false

        fun readDigits() {
            while (true) {
                val ch = peek() ?: break
                if (!ch.isDigit()) {
                    break
                }
                sawDigit = true
                raw.append(next())
            }
        }

        if (peek() == '.') {
            raw.append(next())
            readDigits()
        } else {
            readDigits()
            if (peek() == '.') {
                raw.append(next())
                readDigits()
            }
        }

        if (!sawDigit) {
            // Rollback-ish message.
            error("Invalid number literal")
        }

        // Exponent.
        val p = peek()
        if (p == 'e' || p == 'E') {
            raw.append(next())
            val sgn = peek()
            if (sgn == '+' || sgn == '-') raw.append(next())
            val expStart = this.index
            var expDigits = 0
            while (true) {
                val ch = peek() ?: break
                if (!ch.isDigit()) {
                    break
                }
                raw.append(next())
                expDigits++
            }
            if (expDigits == 0) {
                resetIndex(expStart)
                error("Invalid exponent (no digits)")
            }
        }

        val lit = raw.toString()

        // BigDecimal doesn't always like ".5" in some JDKs; normalize to "0.5".
        val normalized = if (lit.startsWith(".") || lit.startsWith("+.") || lit.startsWith("-.")) {
            lit.replaceFirst(".", "0.")
        } else {
            lit
        }

        return try {
            CasonNumber.finite(BigDecimal(normalized))
        } catch (_: Exception) {
            // Helpful context.
            val around = this.string.substring(maxOf(0, startIdx), minOf(this.string.length, startIdx + 30))
            error("Invalid number literal '$lit' (around '$around')")
        }
    }

    private fun CharReader.skipWsAndComments() {
        while (true) {
            // whitespace.
            var moved = false
            while (true) {
                val c = peek() ?: break
                if (isWs(c) || isLineTerminator(c)) {
                    next()
                    moved = true
                } else {
                    break
                }
            }

            // comments.
            if (peek() == '/' && peek2() == '/') {
                // line comment.
                nextTwice()
                while (true) {
                    val c = peek() ?: break
                    if (isLineTerminator(c)) {
                        break
                    }
                    next()
                }
                continue
            }
            if (peek() == '/' && peek2() == '*') {
                // block comment.
                nextTwice()

                while (true) {
                    val c = next() ?: error("Unterminated block comment")
                    if (c == '*' && peek() == '/') {
                        next()
                        break
                    }
                }
                continue
            }

            if (!moved) {
                break
            }
        }
    }

    private fun CharReader.consumeLineTerminator() {
        val c = peek() ?: return
        if (c == '\r') {
            next()
            if (peek() == '\n') {
                next()
            }
            return
        }
        if (c == '\n' || c == '\u2028' || c == '\u2029') {
            next()
        }
    }

    private fun CharReader.peekStartsWith(lit: String): Boolean {
        if (this.index + lit.length > this.string.length) {
            return false
        }
        for (k in lit.indices) {
            if (lit[k] != this.string[this.index + k]) {
                return false
            }
        }
        return true
    }

    private fun CharReader.consumeLiteral(lit: String) {
        if (!peekStartsWith(lit)) {
            error("Expected literal '$lit'")
        }
        repeat(lit.length) {
            next()
        }
    }

    private fun Char.isHexDigit(): Boolean = (this in '0'..'9') || (this in 'a'..'f') || (this in 'A'..'F')

    private fun hexVal(c: Char): Int = when (c) {
        in '0'..'9' -> c.code - '0'.code
        in 'a'..'f' -> 10 + (c.code - 'a'.code)
        in 'A'..'F' -> 10 + (c.code - 'A'.code)
        else -> -1
    }

    private fun isWs(c: Char): Boolean =
        c == ' ' || c == '\t' || c == '\u000B' || c == '\u000C' ||
                c == '\u00A0' || c == '\uFEFF' || Character.isWhitespace(c)

    private fun isLineTerminator(c: Char): Boolean =
        c == '\n' || c == '\r' || c == '\u2028' || c == '\u2029'

    private fun isIdStart(c: Char): Boolean =
        c == '$' || c == '_' || Character.isJavaIdentifierStart(c)

    private fun isIdPart(c: Char): Boolean =
        c == '$' || c == '_' || Character.isJavaIdentifierPart(c)

    private fun CharReader.parseIdentifier(): String {
        val c = peek() ?: error("Expected identifier but got EOF")
        if (!isIdStart(c)) {
            error("Invalid identifier start '$c'")
        }
        val builder = StringBuilder()
        builder.append(next())
        while (true) {
            val ch = peek() ?: break
            if (!isIdPart(ch)) {
                break
            }
            builder.append(next())
        }
        return builder.toString()
    }

    fun writeValue(builder: StringBuilder, element: JSONElement, pretty: Boolean, indent: String, depth: Int) {
        when (element) {
            is JSONNull -> builder.append("null")
            is JSONBoolean -> {
                if (element.value) {
                    builder.append("true")
                } else {
                    builder.append("false")
                }
            }

            is JSONString -> builder.append(renderString(element.asString()))
            is JSONNumber -> builder.append(element.toString())
            is JSONArray -> writeArray(builder, element, pretty, indent, depth)
            is JSONObject -> writeObject(builder, element, pretty, indent, depth)
        }
    }

    private fun writeArray(builder: StringBuilder, array: JSONArray, pretty: Boolean, indent: String, depth: Int) {
        builder.append(array.toString(pretty, indent, depth))
    }

    private fun writeObject(builder: StringBuilder, obj: JSONObject, pretty: Boolean, indent: String, depth: Int) {
        builder.append(obj.toString(pretty, indent, depth))
    }

    fun renderKey(key: String): String {
        return if (isSafeUnquotedKey(key) && JSONSettings.serializeVersion == JSONSerializeVersion.JSON5) {
            key
        } else {
            renderString(key)
        }
    }

    private fun isSafeUnquotedKey(key: String): Boolean {
        if (key.isEmpty()) {
            return false
        }
        val first = key[0]
        if (isIdStart(first)) {
            for (i in 1 until key.length) {
                if (!isIdPart(key[i])) {
                    return false
                }
            }
            return true
        }
        return false
    }

    private fun renderString(s: String): String {
        val quote = if (JSONSettings.preferSingleQuote) {
            '\''
        } else {
            '"'
        }
        val builder = StringBuilder()
        builder.append(quote)
        for (ch in s) {
            when (ch) {
                '\\' -> builder.append("\\\\")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                '\u000B' -> builder.append("\\v")
                '\'' -> {
                    if (quote == '\'') {
                        builder.append("\\'")
                    } else {
                        builder.append('\'')
                    }
                }

                '"' -> {
                    if (quote == '"') {
                        builder.append("\\\"")
                    } else {
                        builder.append('"')
                    }
                }

                '\u2028' -> builder.append("\\u2028")
                '\u2029' -> builder.append("\\u2029")
                else -> {
                    if (ch.code < 0x20) {
                        builder.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        builder.append(ch)
                    }
                }
            }
        }
        builder.append(quote)
        return builder.toString()
    }
}

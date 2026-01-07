package com.github.cao.awa.cason.parser

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
        val reader = CharReader(
            chars = chars,
            start = 0,
            end = chars.size,
            isFinal = true
        )
        val element = reader.parseElement()
        reader.skipWsAndComments()
        if (!reader.eof()) {
            reader.error("Trailing characters after top-level value")
        }
        return element
    }

    // Value type dispatch table for ASCII.
    // 0=other, 1=obj, 2=arr, 3=str, 4=num .
    private val VALUE_TYPE = ByteArray(128).apply {
        this['{'.code] = 1
        this['['.code] = 2
        this['"'.code] = 3
        this['\''.code] = 3
        this['+'.code] = 4
        this['-'.code] = 4
        this['.'.code] = 4
        for (c in '0'.code..'9'.code) this[c] = 4
    }

    // ASCII identifier char classes to reduce calls into util for common cases.
    private val ASCII_ID_START = BooleanArray(128).apply {
        // JS IdentifierStart includes $, _, letters. (We keep it conservative and defer to util for non-ascii).
        this['_'.code] = true
        this['$'.code] = true
        for (c in 'A'.code..'Z'.code) this[c] = true
        for (c in 'a'.code..'z'.code) this[c] = true
    }
    private val ASCII_ID_PART = BooleanArray(128).apply {
        for (i in ASCII_ID_START.indices) this[i] = ASCII_ID_START[i]
        for (c in '0'.code..'9'.code) this[c] = true
    }

    fun CharReader.parseElement(): JSONElement {
        skipWsAndComments()
        val c = peek() ?: run {
            if (this.isFinal) {
                error("Expected a value but got EOF")
            }
            throw NeedMoreInputException(this)
        }

        val t = if (c.code < 128) VALUE_TYPE[c.code].toInt() else 0
        return when (t) {
            1 -> parseObject()
            2 -> parseArray()
            3 -> JSONString(parseString())
            4 -> JSONNumber(parseNumber())
            else -> parseIdentifierValueOrError(c)
        }
    }

    private fun CharReader.parseIdentifierValueOrError(c: Char): JSONElement {
        if (isIdStart(c)) {
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
        skipWsAndComments()

        val map = LinkedHashMap<String, JSONElement>(8)
        if (peek() == '}') {
            next()
            return JSONObject(map)
        }

        while (true) {
            val key = parseObjectKey()
            skipWsAndComments()
            expect(':')
            val value = parseElement()
            map[key] = value
            skipWsAndComments()

            when (peek()) {
                ',' -> {
                    next()
                    skipWsAndComments()
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
        val c = peek() ?: run {
            if (this.isFinal) {
                error("Expected object key but got EOF")
            }
            throw NeedMoreInputException(this)
        }

        return when {
            c == '"' || c == '\'' -> parseString()
            c == '+' || c == '-' || c == '.' || c.isDigit() -> parseNumber().toString()
            isIdStart(c) -> parseIdentifierFast()
            else -> error("Invalid object key start '$c'")
        }
    }

    private fun CharReader.parseArray(): JSONArray {
        expect('[')
        skipWsAndComments()

        val list = ArrayList<JSONElement>(8)
        if (peek() == ']') {
            next()
            return JSONArray(list)
        }

        while (true) {
            list.add(parseElement())
            skipWsAndComments()
            when (peek()) {
                ',' -> {
                    next()
                    skipWsAndComments()
                    if (peek() == ']') {
                        next()
                        break
                    }
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

    private fun CharReader.parseString(): String {
        val quote = next() ?: run {
            if (this.isFinal) {
                error("Expected quote but got EOF")
            }
            throw NeedMoreInputException(this)
        }
        if (quote != '"' && quote != '\'') {
            error("Internal: parseString called at non-quote")
        }

        val contentStart = index
        var builder: StringBuilder? = null

        while (true) {
            if (this.index >= this.end) {
                if (this.isFinal) {
                    error("Unterminated string")
                }
                throw NeedMoreInputException(this)
            }

            val c = this.chars[this.index]

            if (c == quote) {
                this.index++
                this.col++
                return builder?.toString() ?: String(this.chars, contentStart, (this.index - 1) - contentStart)
            }

            // Raw line terminator not allowed.
            if (c == '\n' || c == '\r' || c == '\u2028' || c == '\u2029') {
                error("Unescaped line terminator in string")
            }

            if (c != '\\') {
                builder?.append(c)
                this.index++
                this.col++
                continue
            }

            // Escape.
            require(2)
            if (builder == null) {
                builder = StringBuilder(16 + (this.index - contentStart))
                builder.appendRange(this.chars, contentStart, contentStart + (this.index - contentStart))
            }

            // Consume backslash.
            this.index++
            this.col++

            val esc = next() ?: run {
                if (this.isFinal) {
                    error("Unterminated escape")
                }
                throw NeedMoreInputException(this)
            }

            // Line continuation: "\" + line terminator => skip terminator.
            if (CasonUtil.isLineTerminator(esc)) {
                continue
            }

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
                    if (peek()?.isDigit() == true) {
                        error("Invalid escape \\0 followed by digit")
                    }
                    builder.append('\u0000')
                }
                'x' -> {
                    require(2)
                    val h1 = next() ?: error("Invalid \\x escape")
                    val h2 = next() ?: error("Invalid \\x escape")
                    val v1 = CasonUtil.hexVal(h1)
                    val v2 = CasonUtil.hexVal(h2)
                    if (v1 < 0 || v2 < 0) {
                        error("Invalid hex in \\x escape")
                    }
                    builder.append(((v1 shl 4) + v2).toChar())
                }
                'u' -> {
                    if (peek() == '{') {
                        next() // Consume ‘{’ .
                        var cp = 0
                        var saw = false
                        while (true) {
                            val ch = next() ?: run {
                                if (this.isFinal) {
                                    error("Unterminated \\u{...} escape")
                                }
                                throw NeedMoreInputException(this)
                            }
                            if (ch == '}') {
                                break
                            }
                            val hv = CasonUtil.hexVal(ch)
                            if (hv < 0) {
                                error("Invalid hex digit in \\u{...}: '$ch'")
                            }
                            cp = (cp shl 4) + hv
                            saw = true
                        }
                        if (!saw) {
                            error("Empty \\u{...} escape")
                        }
                        builder.appendCodePoint(cp)
                    } else {
                        var code = 0
                        repeat(4) {
                            val ch = next() ?: run {
                                if (this.isFinal) {
                                    error("Invalid \\u escape")
                                }
                                throw NeedMoreInputException(this)
                            }
                            val hv = CasonUtil.hexVal(ch)
                            if (hv < 0) {
                                error("Invalid hex digit in \\u escape: '$ch'")
                            }
                            code = (code shl 4) + hv
                        }
                        builder.append(code.toChar())
                    }
                }
                else -> error("Unknown escape sequence: \\$esc")
            }
        }
    }

    private fun CharReader.parseNumber(): CasonNumber {
        skipWsAndComments()
        val startIdx = this.index

        // Optional sign.
        var signChar: Char? = null
        val p0 = peek()
        if (p0 == '+' || p0 == '-') {
            signChar = next()
        }

        // Infinity/NaN (fast literal match).
        if (matchLiteral("Infinity")) {
            consumeLiteral("Infinity")
            return if (signChar == '-') {
                CasonNumber.negInf()
            } else {
                CasonNumber.posInf()
            }
        }
        if (matchLiteral("NaN")) {
            consumeLiteral("NaN")
            return CasonNumber.nan()
        }

        // Hex: 0x...
        if (peek() == '0' && (peek2() == 'x' || peek2() == 'X')) {
            require(2)
            next()
            next() // 0x
            var saw = false
            var bi = BigInteger.ZERO
            while (true) {
                val ch = peek() ?: break
                val hv = CasonUtil.hexVal(ch)
                if (hv < 0) {
                    break
                }
                saw = true
                bi = bi.shiftLeft(4).add(BigInteger.valueOf(hv.toLong()))
                next()
            }
            if (!saw) {
                error("Invalid hex literal (no digits)")
            }
            val bd = BigDecimal(bi)
            return if (signChar == '-') {
                CasonNumber.finite(bd.negate())
            } else {
                CasonNumber.finite(bd)
            }
        }

        val numStart = this.index

        // Integer fast path (only if no '.' and no exp, and fits Long).
        var intValue = 0L
        var sawDigit = false
        var isInteger = true
        var hasDot = false
        var hasExp = false

        fun readDigits() {
            while (true) {
                val ch = peek() ?: break
                if (!ch.isDigit()) {
                    break
                }
                sawDigit = true
                if (isInteger) {
                    val d = ch.code - 48
                    val n = intValue * 10 + d
                    if (n < intValue) {
                        isInteger = false
                        next()
                    } else {
                        intValue = n
                        next()
                    }
                } else {
                    next()
                }
            }
        }

        if (peek() == '.') {
            hasDot = true
            isInteger = false
            next()
            while (peek()?.isDigit() == true) {
                sawDigit = true
                next()
            }
        } else {
            readDigits()
            if (peek() == '.') {
                hasDot = true
                isInteger = false
                next()
                while (peek()?.isDigit() == true) {
                    sawDigit = true
                    next()
                }
            }
        }

        if (!sawDigit) {
            error("Invalid number literal")
        }

        val pe = peek()
        if (pe == 'e' || pe == 'E') {
            hasExp = true
            isInteger = false
            next()
            val sgn = peek()
            if (sgn == '+' || sgn == '-') next()
            val firstExp = peek()
            if (firstExp == null) {
                if (this.isFinal) {
                    error("Invalid exponent (no digits)")
                }
                throw NeedMoreInputException(this)
            }
            if (!firstExp.isDigit()) {
                error("Invalid exponent (no digits)")
            }
            while (peek()?.isDigit() == true) {
                next()
            }
        }

        val after = index

        // Fast int result (no allocations).
        if (isInteger && !hasDot && !hasExp) {
            val signed = if (signChar == '-') {
                -intValue
            } else {
                intValue
            }
            return CasonNumber.finite(BigDecimal.valueOf(signed))
        }

        // Slow path: single slice + BigDecimal(String).
        val raw = String(chars, numStart, after - numStart)
        val lit = if (signChar == null) {
            raw
        } else {
            "$signChar$raw"
        }

        // Normalize ".5" / "+.5" / "-.5" .
        val normalized = if (lit.startsWith(".") || lit.startsWith("+.") || lit.startsWith("-.")) {
            lit.replaceFirst(".", "0.")
        } else {
            lit
        }

        return try {
            CasonNumber.finite(BigDecimal(normalized))
        } catch (_: Exception) {
            val around = String(chars, startIdx, minOf(end - startIdx, 30))
            error("Invalid number literal '$lit' (around '$around')")
        }
    }

    fun CharReader.skipWsAndComments() {
        while (true) {
            var moved = false

            while (true) {
                val c = peek() ?: break
                if (CasonUtil.isWs(c) || CasonUtil.isLineTerminator(c)) {
                    next()
                    moved = true
                } else break
            }

            // line comment //
            if (peek() == '/' && peek2() == '/') {
                require(2)
                next()
                next()
                while (true) {
                    val c = peek() ?: break
                    if (CasonUtil.isLineTerminator(c)) break
                    next()
                }
                continue
            }

            // block comment /* ... */
            if (peek() == '/' && peek2() == '*') {
                require(2)
                next()
                next()
                while (true) {
                    val c = next() ?: run {
                        if (this.isFinal) {
                            error("Unterminated block comment")
                        }
                        throw NeedMoreInputException(this)
                    }
                    if (c == '*' && peek() == '/') {
                        next()
                        break
                    }
                }
                continue
            }

            if (!moved) break
        }
    }

    private fun isIdStart(c: Char): Boolean {
        val code = c.code
        return if (code < 128) ASCII_ID_START[code] else CasonUtil.isIdStart(c)
    }

    private fun isIdPart(c: Char): Boolean {
        val code = c.code
        return if (code < 128) ASCII_ID_PART[code] else CasonUtil.isIdPart(c)
    }

    private fun CharReader.parseIdentifierFast(): String {
        // Scan in current buffer.
        require(1)
        val s = this.index
        val first = this.chars[this.index]
        if (!isIdStart(first)) {
            error("Invalid identifier start '$first'")
        }

        // Consume first.
        this.index++
        this.col++

        while (this.index < this.end) {
            val c = this.chars[this.index]
            if (!isIdPart(c)) {
                break
            }
            this.index++
            this.col++
        }

        // If chunk ended in the middle of an identifier, and not final, we must ask for more
        if (!this.isFinal && this.index >= this.end) {
            throw NeedMoreInputException(this)
        }

        return String(this.chars, s, this.index - s)
    }

    private fun CharReader.matchLiteral(lit: String): Boolean {
        val n = lit.length
        if (this.index + n > this.end) {
            if (this.isFinal) return false
            throw NeedMoreInputException(this)
        }
        for (i in 0 until n) {
            if (this.chars[this.index + i] != lit[i]) return false
        }
        return true
    }

    private fun CharReader.consumeLiteral(lit: String) {
        if (!matchLiteral(lit)) error("Expected literal '$lit'")
        // consume without extra checks
        for (i in lit.indices) {
            next()
        }
    }
}

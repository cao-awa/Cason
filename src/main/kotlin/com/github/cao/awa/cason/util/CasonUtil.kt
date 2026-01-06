package com.github.cao.awa.cason.util

import com.github.cao.awa.cason.reader.CharReader

object CasonUtil {
    fun Char.isHexDigit(): Boolean = (this in '0'..'9') || (this in 'a'..'f') || (this in 'A'..'F')

    fun hexVal(c: Char): Int = when (c) {
        in '0'..'9' -> c.code - '0'.code
        in 'a'..'f' -> 10 + (c.code - 'a'.code)
        in 'A'..'F' -> 10 + (c.code - 'A'.code)
        else -> -1
    }

    fun isWs(c: Char): Boolean =
        c == ' ' || c == '\t' || c == '\u000B' || c == '\u000C' ||
                c == '\u00A0' || c == '\uFEFF' || Character.isWhitespace(c)

    fun isLineTerminator(c: Char): Boolean =
        c == '\n' || c == '\r' || c == '\u2028' || c == '\u2029'

    fun isIdStart(c: Char): Boolean =
        c == '$' || c == '_' || Character.isJavaIdentifierStart(c)

    fun isIdPart(c: Char): Boolean =
        c == '$' || c == '_' || Character.isJavaIdentifierPart(c)

    fun CharReader.parseIdentifier(): String {
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
}
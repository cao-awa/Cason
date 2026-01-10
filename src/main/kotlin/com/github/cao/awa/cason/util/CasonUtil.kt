package com.github.cao.awa.cason.util

object CasonUtil {
    fun isWs(c: Char): Boolean =
        c == ' ' || c == '\t' || c == '\u000B' || c == '\u000C' ||
                c == '\u00A0' || c == '\uFEFF' || Character.isWhitespace(c)

    fun isIdStart(c: Char): Boolean =
        c == '$' || c == '_' || Character.isJavaIdentifierStart(c)

    fun isIdPart(c: Char): Boolean =
        c == '$' || c == '_' || Character.isJavaIdentifierPart(c)
}
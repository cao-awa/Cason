package com.github.cao.awa.cason.reader

import com.github.cao.awa.cason.exception.JSONParseException
import com.github.cao.awa.cason.exception.NeedMoreInputException

class CharReader(
    val chars: CharArray,
    val start: Int,
    val end: Int,
    val isFinal: Boolean,
    val baseIndex: Int = 0,
    baseLine: Int = 1,
    baseCol: Int = 1
) {
    var index: Int = start
    var line: Int = baseLine
    var col: Int = baseCol

    fun eof(): Boolean = this.index >= this.end

    fun require(n: Int = 1) {
        if (this.index + (n - 1) >= this.end) {
            if (this.isFinal) {
                error("Unexpected EOF")
            }
            throw NeedMoreInputException(this)
        }
    }

    fun peek(): Char? = if (this.index < this.end) {
        this.chars[this.index]
    } else {
        null
    }

    fun peek2(): Char? = if (this.index + 1 < this.end) {
        this.chars[this.index + 1]
    } else {
        null
    }

    fun next(): Char? {
        if (index >= end) {
            if (isFinal) return null
            throw NeedMoreInputException(this)
        }
        val c = chars[index++]
        advanceLineCol(c)
        return c
    }

    fun advanceLineCol(c: Char) {
        when (c) {
            '\n', '\u2028', '\u2029' -> {
                this.line++
                this.col = 1
            }
            '\r' -> {
                // \r\n treat as single line break.
                if (this.index < this.end) {
                    if (this.chars[this.index] == '\n') this.index++
                } else if (!this.isFinal) {
                    // Chunk ends after \r, need more to see if next is \n .
                    throw NeedMoreInputException(this)
                }
                this.line++
                this.col = 1
            }
            else -> this.col++
        }
    }

    fun expect(ch: Char) {
        require(1)
        val got = next() ?: error("Expected '$ch' but got EOF")
        if (got != ch) {
            error("Expected '$ch' but got '$got'")
        }
    }

    fun error(msg: String): Nothing {
        val abs = this.baseIndex + (this.index - this.start)
        val aroundStart = maxOf(this.start, this.index - 20)
        val aroundEnd = minOf(this.end, this.index + 20)
        val excerpt = String(this.chars, aroundStart, aroundEnd - aroundStart).replace("\n", "\\n")
        throw JSONParseException(
            "JSON5 Parse Error @ line ${this.line}, col ${this.col}, index $abs: $msg. Around: \"$excerpt\""
        )
    }
}
package com.github.cao.awa.cason.reader

import com.github.cao.awa.cason.exception.NeedMoreInputException
import com.github.cao.awa.cason.util.CasonUtil

class CharReader(
    val chars: CharArray,
    var index: Int,
    val end: Int,
    val isFinal: Boolean
) {
    var line: Int = 1
    var col: Int = 1

    fun eof(): Boolean = this.index >= this.end

    fun ensure(n: Int = 1) {
        if (this.index + (n - 1) >= this.end) {
            if (this.isFinal) {
                error("Unexpected EOF")
            }
            throw NeedMoreInputException(this)
        }
    }

    fun peek(): Char = this.chars[this.index]

    fun next(): Char {
        val c = this.chars[this.index++]
        this.col++
        return c
    }

    fun skip(n: Int) {
        this.index += n
        this.col += n
    }

    fun expect(ch: Char) {
        ensure(1)
        val c = this.chars[this.index++]
        this.col++
        if (c != ch) error("Expected '$ch' but got '$c'")
    }

    fun advanceLineCol(c: Char) {
        when (c) {
            '\n', '\u2028', '\u2029' -> {
                this.line++
                this.col = 1
            }
            '\r' -> {
                if (this.index < this.end && this.chars[this.index] == '\n') {
                    this.index++
                } else if (this.index >= this.end && !this.isFinal) {
                    throw NeedMoreInputException(this)
                }
                this.line++
                this.col = 1
            }
            else -> this.col++
        }
    }

    fun error(msg: String): Nothing {
        throw IllegalArgumentException("$msg at line $this.line, column $this.col")
    }

    fun skipWs() {
        while (true) {
            val c = peek()
            if (CasonUtil.isWs(c) || CasonUtil.isLineTerminator(c)) {
                next()
            } else {
                return
            }
        }
    }
}

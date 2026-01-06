package com.github.cao.awa.cason.reader

class CharReader(val string: String) {
    var index: Int = 0
        private set
    var line: Int = 1
        private set
    var col: Int = 1
        private set

    fun eof(): Boolean = this.index >= this.string.length
    fun peek(): Char? {
        return if (eof()) {
            null
        } else {
            this.string[index]
        }
    }

    fun peek2(): Char? {
        return if (this.index + 1 >= this.string.length) {
            null
        } else {
            this.string[index + 1]
        }
    }

    fun next(): Char? {
        if (eof()) {
            return null
        }
        val c = this.string[this.index++]
        if (c == '\n') {
            this.line++
            this.col = 1
        } else {
            this.col++
        }
        return c
    }

    fun nextTwice(): Char? {
        next()
        return next()
    }

    fun expect(ch: Char) {
        val c = next() ?: error("Expected '$ch' but got EOF")
        if (c != ch) {
            error("Expected '$ch' but got '$c'")
        }
    }

    fun resetIndex(index: Int) {
        this.index = index
    }
}
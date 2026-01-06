package com.github.cao.awa.caoson.reader

class CharReader(val string: String) {
    var index: Int = 0
    var line: Int = 1
        private set
    var col: Int = 1
        private set

    fun eof(): Boolean = index >= string.length
    fun peek(): Char? = if (eof()) null else string[index]
    fun peek2(): Char? = if (index + 1 >= string.length) null else string[index + 1]

    fun next(): Char? {
        if (eof()) return null
        val c = string[index++]
        if (c == '\n') { line++; col = 1 }
        else { col++ }
        return c
    }

    fun nextTwice() {
        next()
        next()
    }

    fun expect(ch: Char) {
        val c = next() ?: error("Expected '$ch' but got EOF")
        if (c != ch) {
            error("Expected '$ch' but got '$c'")
        }
    }
}
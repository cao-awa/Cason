package com.github.cao.awa.cason.util.bytes

class BytesReader(bytes: ByteArray?, cursor: Int) {
    private var flag = 0
    private var bytes: ByteArray
    var cursor: Int = cursor
        get() = field
        private set

    init {
        this.bytes = bytes ?: BytesUtil.EMPTY
    }

    fun reverseRound(round: Int, length: Int): ByteArray {
        val bytes: ByteArray? = read(length)
        val result = ByteArray(round)

        if (length >= 0) {
            System.arraycopy(
                bytes,
                0,
                result,
                round - length,
                length
            )
        }
        return result
    }

    fun read(length: Int): ByteArray {
        if (length == 0) {
            return BytesUtil.EMPTY
        }
        if (length + this.cursor > this.bytes.size) {
            return ByteArray(length)
        } else {
            val result = ByteArray(length)
            System.arraycopy(
                this.bytes,
                this.cursor,
                result,
                0,
                result.size
            )
            this.cursor += length
            return result
        }
    }

    fun next(target: Int): BytesReader {
        while (read() == target) {
        }
        this.cursor--
        return this
    }

    fun skip(length: Int): BytesReader {
        this.cursor += length
        return this
    }

    fun readable(length: Int): Boolean {
        return readable() >= length
    }

    fun read(): Int {
        return if (this.bytes.size > this.cursor) this.bytes[this.cursor++].toInt() else -1
    }

    fun reset(): BytesReader {
        this.cursor = 0
        this.flag = 0
        return this
    }

    fun reset(newBytes: ByteArray): BytesReader {
        this.bytes = newBytes
        return reset()
    }

    fun back(range: Int): BytesReader {
        this.cursor -= range
        return this
    }

    fun back(): BytesReader {
        this.cursor = this.flag
        return this
    }

    fun all(): ByteArray? {
        return read(this.bytes.size - this.cursor)
    }

    fun non(): ByteArray {
        return BytesUtil.EMPTY
    }

    fun flag(): Int {
        this.flag = this.cursor
        return this.flag
    }

    fun readable(): Int {
        return this.bytes.size - this.cursor
    }

    fun length(): Int {
        return this.bytes.size
    }

    companion object {
        fun of(bytes: ByteArray?): BytesReader {
            return BytesReader(bytes, 0)
        }
    }
}
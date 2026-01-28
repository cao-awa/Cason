package com.github.cao.awa.cason.util.math

import com.github.cao.awa.cason.util.bytes.BytesReader
import com.github.cao.awa.cason.util.bytes.BytesUtil
import com.github.cao.awa.cason.util.math.Base256.intFromBuf
import com.github.cao.awa.cason.util.math.Base256.longFromBuf

object SkippedBase256 {
    fun readLong(reader: BytesReader, length: Int): Long {
        return longFromBuf(
            reader.reverseRound(
                8,
                length
            )
        )
    }

    fun readLong(bytes: ByteArray): Long {
        return readLong(BytesReader(bytes, 0))
    }

    fun readLong(reader: BytesReader): Long {
        val length: Int = reader.read()
        if (length == -1) {
            return longFromBuf(reader.read(8))
        } else if (length == 0) {
            return 0
        }
        return longFromBuf(
            reader.reverseRound(
                8,
                length
            )
        )
    }

    fun readInt(bytes: ByteArray): Int {
        return readInt(BytesReader(bytes, 0))
    }

    fun readInt(reader: BytesReader): Int {
        val length: Int = reader.read()
        if (length == -1) {
            return intFromBuf(reader.read(4))
        } else if (length == 0) {
            return 0
        }
        return intFromBuf(
            reader.reverseRound(
                4,
                length
            )
        )
    }

    fun longToBuf(l: Long): ByteArray {
        return skip(Base256.longToBuf(l))
    }

    fun intToBuf(l: Int): ByteArray {
        return skip(Base256.intToBuf(l))
    }

    fun tagToBuf(l: Int): ByteArray {
        return skip(Base256.tagToBuf(l))
    }

    fun skip(buf: ByteArray): ByteArray {
        if (buf[0].toInt() == 0 && buf[1].toInt() == 0) {
            val skd: ByteArray = BytesUtil.skp(
                buf,
                0.toByte()
            )
            val result = ByteArray(skd.size + 1)
            result[0] = skd.size.toByte()
            System.arraycopy(
                skd,
                0,
                result,
                1,
                skd.size
            )
            return result
        }
        return BytesUtil.concat(
            byteArrayOf(-1),
            buf
        )
    }
}
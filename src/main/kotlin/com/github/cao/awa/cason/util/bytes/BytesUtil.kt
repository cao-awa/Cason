package com.github.cao.awa.cason.util.bytes

object BytesUtil {
    val EMPTY: ByteArray = ByteArray(0)

    fun xor(target: ByteArray, xor: ByteArray) {
        for (i in target.indices) {
            target[i] = (target[i].toInt() xor xor[i].toInt()).toByte()
        }
    }

    fun reverse(bytes: ByteArray) {
        var start = 0
        var end = bytes.size - 1
        while (start < end) {
            bytes[end] = (bytes[end].toInt() xor bytes[start].toInt()).toByte()
            bytes[start] = (bytes[start].toInt() xor bytes[end].toInt()).toByte()
            bytes[end] = (bytes[end].toInt() xor bytes[start].toInt()).toByte()
            start++
            end--
        }
    }

    fun skp(bytes: ByteArray, target: Byte): ByteArray {
        for (i in bytes.indices) {
            if (bytes[i] == target) {
                continue
            }
            val result = ByteArray(bytes.size - i)
            System.arraycopy(
                bytes,
                i,
                result,
                0,
                result.size
            )
            return result
        }

        return ByteArray(0)
    }

    @JvmOverloads
    fun skd(bytes: ByteArray, target: Byte, fill: Byte = 0.toByte()) {
        var i = 0
        while (i < bytes.size) {
            if (bytes[i] == target) {
                i++
                continue
            }
            for (i1 in bytes.indices) {
                if (i < bytes.size) {
                    bytes[i1] = bytes[i++]
                } else {
                    bytes[i1] = fill
                }
            }
            i++
        }
    }

    fun concat(vararg arrays: ByteArray): ByteArray {
        var length = 0
        for (array in arrays) {
            length += array.size
        }
        val result = ByteArray(length)
        var cur = 0
        for (array in arrays) {
            if (array.size == 0) {
                continue
            }
            System.arraycopy(
                array,
                0,
                result,
                cur,
                array.size
            )
            cur += array.size
        }
        return result
    }

    fun arrau(vararg bytes: Byte): ByteArray? {
        return bytes
    }

    fun arrau(vararg bytes: Int): ByteArray {
        val result = ByteArray(bytes.size)
        var index = 0
        for (i in bytes) {
            result[index++] = i.toByte()
        }
        return result
    }
}
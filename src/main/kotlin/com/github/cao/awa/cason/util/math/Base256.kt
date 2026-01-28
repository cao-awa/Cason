package com.github.cao.awa.cason.util.math

object Base256 {
    /**
     * Convert a long to 8 bytes.
     *
     * @param l Long input
     * @param buf Bytes output
     * @return Bytes result
     *
     * @author InkerBot
     * @author cao_awa
     * @since 1.0.0
     */
    /**
     * Convert a long to 8 bytes.
     *
     * @param l
     * Long input
     * @return Bytes result
     *
     * @author cao_awa
     * @since 1.0.0
     */
    @JvmOverloads
    fun longToBuf(l: Long, buf: ByteArray = ByteArray(8)): ByteArray {
        buf[0] = (l ushr 56).toByte()
        buf[1] = (l ushr 48).toByte()
        buf[2] = (l ushr 40).toByte()
        buf[3] = (l ushr 32).toByte()
        buf[4] = (l ushr 24).toByte()
        buf[5] = (l ushr 16).toByte()
        buf[6] = (l ushr 8).toByte()
        buf[7] = (l).toByte()
        return buf
    }

    /**
     * Convert 8 bytes to a long.
     *
     * @param buf Bytes input
     * @return Long result
     *
     * @author InkerBot
     * @author cao_awa
     * @since 1.0.0
     */
    fun longFromBuf(buf: ByteArray): Long {
        return ((buf[0].toLong() and 0xFFL) shl 56) +
                ((buf[1].toLong() and 0xFFL) shl 48) +
                ((buf[2].toLong() and 0xFFL) shl 40) +
                ((buf[3].toLong() and 0xFFL) shl 32) +
                ((buf[4].toLong() and 0xFFL) shl 24) +
                ((buf[5].toLong() and 0xFFL) shl 16) +
                ((buf[6].toLong() and 0xFFL) shl 8) +
                ((buf[7].toLong() and 0xFFL))
    }

    /**
     * Convert an integer to 4 bytes.
     *
     * @param i Integer input
     * @param buf Bytes output
     * @return Bytes result
     *
     * @author InkerBot
     * @author cao_awa
     * @since 1.0.0
     */
    /**
     * Convert an integer to 4 bytes.
     *
     * @param i
     * Integer input
     * @return Bytes result
     *
     * @author cao_awa
     * @since 1.0.0
     */
    @JvmOverloads
    fun intToBuf(i: Int, buf: ByteArray = ByteArray(4)): ByteArray {
        buf[0] = (i ushr 24).toByte()
        buf[1] = (i ushr 16).toByte()
        buf[2] = (i ushr 8).toByte()
        buf[3] = (i).toByte()
        return buf
    }

    /**
     * Convert 4 bytes to an integer.
     *
     * @param buf Bytes input
     * @return Integer result
     *
     * @author InkerBot
     * @author cao_awa
     * @since 1.0.0
     */
    fun intFromBuf(buf: ByteArray): Int {
        return (((buf[0].toInt() and 0xFF) shl 24) +
                ((buf[1].toInt() and 0xFF) shl 16) +
                ((buf[2].toInt() and 0xFF) shl 8) +
                ((buf[3].toInt() and 0xFF)))
    }

    /**
     * Convert 2 bytes to an integer.
     *
     * @param buf Bytes input
     * @return Integer result
     *
     * @author cao_awa
     * @since 1.0.0
     */
    fun tagFromBuf(buf: ByteArray): Int {
        return (((buf[0].toInt() and 0xFF) shl 8) +
                ((buf[1].toInt() and 0xFF)))
    }

    /**
     * Convert an integer to 2 bytes.
     *
     * @param i Integer input
     * @param buf Bytes output
     * @return Bytes result
     *
     * @author cao_awa
     * @since 1.0.0
     */
    /**
     * Convert an integer to 2 bytes.
     *
     * @param i
     * Integer input
     * @return Bytes result
     *
     * @author cao_awa
     * @since 1.0.0
     */
    @JvmOverloads
    fun tagToBuf(i: Int, buf: ByteArray = ByteArray(2)): ByteArray {
        buf[0] = (i ushr 8).toByte()
        buf[1] = (i).toByte()
        return buf
    }
}
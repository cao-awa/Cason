package com.github.cao.awa.cason.binary.compress

import com.github.cao.awa.cason.util.bytes.BytesUtil
import java.io.*
import java.util.zip.DeflaterInputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import java.util.zip.InflaterOutputStream

object BinaryCompress {
    fun compress(
        bytes: ByteArray
    ): ByteArray {
        if (bytes.isEmpty()) {
            return BytesUtil.EMPTY
        }
        try {
            ByteArrayOutputStream().use { out ->
                DeflaterOutputStream(out).use {
                    it.write(bytes)
                    it.close()
                }
                return out.toByteArray()
            }
        } catch (ex: Exception) {
            return bytes
        }
    }

    fun decompress(
        bytes: ByteArray
    ): ByteArray {
        if (bytes.isEmpty()) {
            return BytesUtil.EMPTY
        }
        try {
            return InflaterInputStream(ByteArrayInputStream(bytes)).readAllBytes()
        } catch (ex: Exception) {
            return bytes
        }
    }
}
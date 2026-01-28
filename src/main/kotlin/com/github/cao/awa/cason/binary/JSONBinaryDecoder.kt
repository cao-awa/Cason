package com.github.cao.awa.cason.binary

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.array.JSONArray
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.primary.JSONBoolean
import com.github.cao.awa.cason.primary.JSONNumber
import com.github.cao.awa.cason.primary.JSONString
import com.github.cao.awa.cason.primary.number.JSONBigDecimal
import com.github.cao.awa.cason.primary.number.JSONByte
import com.github.cao.awa.cason.primary.number.JSONDouble
import com.github.cao.awa.cason.primary.number.JSONFloat
import com.github.cao.awa.cason.primary.number.JSONInt
import com.github.cao.awa.cason.primary.number.JSONLong
import com.github.cao.awa.cason.primary.number.JSONShort
import com.github.cao.awa.cason.primary.number.infinity.JSONNegativeInfinity
import com.github.cao.awa.cason.primary.number.infinity.JSONPositiveInfinity
import com.github.cao.awa.cason.primary.number.nan.JSONNaN
import com.github.cao.awa.cason.util.math.Base256
import com.github.cao.awa.cason.util.math.SkippedBase256
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.KClass

class JSONBinaryDecoder {
    companion object {
        fun decodeElement(tag: Int, input: InputStream): JSONElement {
            return when (tag) {
                0 -> decodeObject(input)
                1 -> decodeArray(input)
                2 -> decodeByte(input)
                3 -> decodeShort(input)
                4 -> decodeInt(input)
                5 -> decodeLong(input)
                6 -> decodeFloat(input)
                7 -> decodeDouble(input)
                8 -> decodeBigDecimal(input)
                9 -> decodeString(input)
                10 -> decodeBoolean(input)
                11 -> JSONNaN
                12 -> JSONPositiveInfinity
                13 -> JSONNegativeInfinity
                else -> error("Unexpected input tag: $tag")
            }
        }

        fun decodeByte(input: InputStream): JSONByte {
            return JSONNumber.ofByte(input.read().toByte())
        }

        fun decodeShort(input: InputStream): JSONShort {
            val short = ByteArray(2)
            input.read(short)
            return JSONNumber.ofShort(Base256.tagFromBuf(short).toShort())
        }

        fun decodeInt(input: InputStream): JSONInt {
            val integer = ByteArray(4)
            input.read(integer)
            return JSONNumber.ofInt(Base256.intFromBuf(integer))
        }

        fun decodeLong(input: InputStream): JSONLong {
            val integer = ByteArray(8)
            input.read(integer)
            return JSONNumber.ofLong(Base256.longFromBuf(integer))
        }

        fun decodeFloat(input: InputStream): JSONFloat {
            val float = ByteArray(4)
            input.read(float)
            return JSONNumber.ofFloat(
                ByteBuffer
                    .wrap(float)
                    .order(ByteOrder.BIG_ENDIAN)
                    .float
            )
        }

        fun decodeDouble(input: InputStream): JSONDouble {
            val double = ByteArray(8)
            input.read(double)
            return JSONNumber.ofDouble(
                ByteBuffer
                    .wrap(double)
                    .order(ByteOrder.BIG_ENDIAN)
                    .double
            )
        }

        fun decodeBigDecimal(input: InputStream): JSONBigDecimal {
            val dataSize = input.read()
            val bytes = ByteArray(dataSize)
            input.read(bytes)
            val buffer = ByteBuffer.wrap(bytes)

            val scale = buffer.int
            val length = buffer.int

            val unscaledBytes = ByteArray(length)
            buffer.get(unscaledBytes)

            val unscaled = BigInteger(unscaledBytes)

            return JSONNumber.ofBig(BigDecimal(unscaled, scale))
        }

        fun decodeString(input: InputStream): JSONString {
            val length = input.read()
            val data = ByteArray(length)
            input.read(data)
            return JSONString(String(data))
        }

        fun decodeObject(input: ByteArray): JSONObject {
            return decodeObject(ByteArrayInputStream(input))
        }

        fun decodeObject(input: InputStream): JSONObject {
            val size = Base256.tagFromBuf(
                ByteArray(2).also {
                    input.read(it)
                }
            )
            val obj = JSONObject()
            for (i in 0 until size) {
                val tag = input.read()
                val keyLength = input.read()
                val keyBytes = ByteArray(keyLength)
                input.read(keyBytes)
                val key = String(keyBytes)
                obj.putElement(key, decodeElement(tag, input))
            }
            return obj
        }

        fun decodeArray(input: InputStream): JSONArray {
            val size = Base256.tagFromBuf(ByteArray(2).also {
                input.read(it)
            })

            val array = JSONArray()
            for (i in 0 until size) {
                val tag = input.read()
                array.add(decodeElement(tag, input))
            }
            return array
        }

        fun decodeBoolean(input: InputStream): JSONBoolean {
            if (input.read() == 0) {
                return JSONBoolean.FALSE
            } else {
                return JSONBoolean.TRUE
            }
        }
    }
}
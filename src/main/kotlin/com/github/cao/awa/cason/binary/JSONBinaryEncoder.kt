package com.github.cao.awa.cason.binary

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.array.JSONArray
import com.github.cao.awa.cason.binary.compress.BinaryCompress
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.primary.JSONBoolean
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
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.KClass

class JSONBinaryEncoder {
    companion object {
        private val elementId: MutableMap<KClass<*>, Int> = mutableMapOf<KClass<*>, Int>().also {
            it[JSONObject::class] = 0
            it[JSONArray::class] = 1
            it[JSONByte::class] = 2
            it[JSONShort::class] = 3
            it[JSONInt::class] = 4
            it[JSONLong::class] = 5
            it[JSONFloat::class] = 6
            it[JSONDouble::class] = 7
            it[JSONBigDecimal::class] = 8
            it[JSONString::class] = 9
            it[JSONBoolean::class] = 10
            it[JSONNaN::class] = 11
            it[JSONPositiveInfinity::class] = 12
            it[JSONNegativeInfinity::class] = 13
        }

        fun encode(value: JSONObject): ByteArray {
            val outputStream = ByteArrayOutputStream()
            outputStream.write(0)
            encodeObject(value, outputStream)
            return BinaryCompress.compress(outputStream.toByteArray())
        }

        fun encode(value: JSONArray): ByteArray {
            val outputStream = ByteArrayOutputStream()
            outputStream.write(1)
            encodeArray(value, outputStream)
            return BinaryCompress.compress(outputStream.toByteArray())
        }

        private fun encodeElement(value: JSONElement, output: OutputStream) {
            when (value) {
                is JSONObject -> encodeObject(value, output)
                is JSONArray -> encodeArray(value, output)
                is JSONByte -> encodeByte(value, output)
                is JSONShort -> encodeShort(value, output)
                is JSONInt -> encodeInt(value, output)
                is JSONLong -> encodeLong(value, output)
                is JSONFloat -> encodeFloat(value, output)
                is JSONDouble -> encodeDouble(value, output)
                is JSONBigDecimal -> encodeBigDecimal(value, output)
                is JSONString -> encodeString(value, output)
                is JSONBoolean -> encodeBoolean(value, output)
            }
        }

        private fun encodeByte(jsonByte: JSONByte, output: OutputStream) {
            output.write(jsonByte.value.toInt())
        }

        private fun encodeShort(jsonShort: JSONShort, output: OutputStream) {
            output.write(Base256.tagToBuf(jsonShort.value.toInt()))
        }

        private fun encodeInt(jsonInt: JSONInt, output: OutputStream) {
            output.write(Base256.intToBuf(jsonInt.value))
        }

        private fun encodeLong(jsonLong: JSONLong, output: OutputStream) {
            output.write(Base256.longToBuf(jsonLong.value))
        }

        private fun encodeFloat(value: JSONFloat, output: OutputStream) {
            output.write(
                ByteBuffer
                    .allocate(4)
                    .order(ByteOrder.BIG_ENDIAN)
                    .putFloat(value.value)
                    .array()
            )
        }

        private fun encodeDouble(value: JSONDouble, output: OutputStream) {
            output.write(
                ByteBuffer
                    .allocate(8)
                    .order(ByteOrder.BIG_ENDIAN)
                    .putDouble(value.value)
                    .array()
            )
        }

        private fun encodeBigDecimal(decimal: JSONBigDecimal, output: OutputStream) {
            val bigDecimal = decimal.value
            val unscaled: BigInteger = bigDecimal.unscaledValue()
            val scale: Int = bigDecimal.scale()

            val unscaledBytes = unscaled.toByteArray()

            val buffer = ByteBuffer.allocate(
                4 + 4 + unscaledBytes.size
            )

            buffer.putInt(scale)
            buffer.putInt(unscaledBytes.size)
            buffer.put(unscaledBytes)

            val byteArray = buffer.array()
            output.write(byteArray.size)
            output.write(byteArray)
        }

        private fun encodeString(jsonString: JSONString, output: OutputStream) {
            val string = jsonString.asString()
            output.write(string.length)
            output.write(string.toByteArray())
        }

        private fun encodeObject(json: JSONObject): ByteArray {
            val output = ByteArrayOutputStream()
            encodeObject(json, output)
            return output.toByteArray()
        }

        private fun encodeObject(json: JSONObject, output: OutputStream) {
            output.write(Base256.tagToBuf(json.size()))
            json.forEach { (key, value) ->
                output.write(this.elementId[value::class]!!)
                output.write(key.length)
                output.write(key.toByteArray())
                encodeElement(value, output)
            }
        }

        private fun encodeArray(array: JSONArray, output: OutputStream) {
            output.write(Base256.tagToBuf(array.size()))

            array.forEach { element ->
                output.write(this.elementId[element::class]!!)
                encodeElement(element, output)
            }
        }

        private fun encodeBoolean(bool: JSONBoolean, output: OutputStream) {
            if (bool.value) {
                output.write(1)
            } else {
                output.write(0)
            }
        }
    }
}
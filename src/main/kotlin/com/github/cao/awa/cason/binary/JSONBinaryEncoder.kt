package com.github.cao.awa.cason.binary

import com.github.cao.awa.cason.array.JSONArray
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

fun main() {
    val json = JSONObject {
        "awa" set 1234
        "nes" nested Test("qaq")
        array("test") {
            +"awa"
            add(JSONObject {
                "awa" set 9876543210
                "1" set true
                "2" set true
                "3" set false
                "4" set false
            })
        }
    }

    val encoded = JSONBinaryEncoder.encodeObject(json)
    println(String(encoded))
    println(encoded.size)
    val decoded = JSONBinaryDecoder.decodeObject(encoded)
    println(decoded.toString(false, ""))
    println(decoded.toString(false, "").length)
}

data class Test(val awa: String) {

}

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

        fun encodeElement(value: Any, output: OutputStream) {
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

        fun encodeByte(jsonByte: JSONByte, output: OutputStream) {
            output.write(jsonByte.value.toInt())
        }

        fun encodeShort(jsonShort: JSONShort, output: OutputStream) {
            output.write(Base256.tagToBuf(jsonShort.value.toInt()))
        }

        fun encodeInt(jsonInt: JSONInt, output: OutputStream) {
            output.write(Base256.intToBuf(jsonInt.value))
        }

        fun encodeLong(jsonLong: JSONLong, output: OutputStream) {
            output.write(Base256.longToBuf(jsonLong.value))
        }

        fun encodeFloat(value: JSONFloat, output: OutputStream) {
            output.write(
                ByteBuffer
                    .allocate(4)
                    .order(ByteOrder.BIG_ENDIAN)
                    .putFloat(value.value)
                    .array()
            )
        }

        fun encodeDouble(value: JSONDouble, output: OutputStream) {
            output.write(
                ByteBuffer
                    .allocate(8)
                    .order(ByteOrder.BIG_ENDIAN)
                    .putDouble(value.value)
                    .array()
            )
        }

        fun encodeBigDecimal(decimal: JSONBigDecimal, output: OutputStream) {
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

        fun encodeString(jsonString: JSONString, output: OutputStream) {
            val string = jsonString.asString()
            output.write(string.length)
            output.write(string.toByteArray())
        }

        fun encodeObject(json: JSONObject): ByteArray {
            val output = ByteArrayOutputStream()
            encodeObject(json, output)
            return output.toByteArray()
        }

        fun encodeObject(json: JSONObject, output: OutputStream) {
            output.write(Base256.tagToBuf(json.size()))
            json.forEach { (key, value) ->
                output.write(this.elementId[value::class]!!)
                output.write(key.length)
                output.write(key.toByteArray())
                encodeElement(value, output)
            }
        }

        fun encodeArray(array: JSONArray, output: OutputStream) {
            output.write(Base256.tagToBuf(array.size()))

            array.forEach { element ->
                output.write(this.elementId[element::class]!!)
                encodeElement(element, output)
            }
        }

        fun encodeBoolean(bool: JSONBoolean, output: OutputStream) {
            if (bool.value) {
                output.write(1)
            } else {
                output.write(0)
            }
        }
    }
}
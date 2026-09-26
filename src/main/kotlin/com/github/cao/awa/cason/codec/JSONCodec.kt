package com.github.cao.awa.cason.codec

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.array.JSONArray
import com.github.cao.awa.cason.codec.decoder.JSONDecoder
import com.github.cao.awa.cason.codec.encoder.JSONEncoder
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.primary.JSONBoolean
import com.github.cao.awa.cason.primary.JSONNull
import com.github.cao.awa.cason.primary.JSONNumber
import com.github.cao.awa.cason.primary.JSONString
import com.github.cao.awa.cason.primary.number.JSONByte
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

@Suppress("unused")
object JSONCodec {
    fun renderJSON(data: JSONObject): String {
        return JSONEncoder.renderJSON(data)
    }

    fun renderJSON(data: JSONObject, pretty: Boolean = false): String {
        return JSONEncoder.renderJSON(data, pretty)
    }

    fun renderArray(data: JSONArray): String {
        return data.toString()
    }

    fun renderArray(data: JSONArray, pretty: Boolean = false): String {
        return data.toString(pretty, "    ", 0)
    }

    fun encode(data: Any?): JSONElement {
        if (data == null) {
            return JSONNull
        }
        return when (data) {
            is String -> JSONString(data)
            is Int -> JSONNumber.ofInt(data)
            is Long -> JSONNumber.ofLong(data)
            is Float -> JSONNumber.ofFloat(data)
            is Double -> JSONNumber.ofDouble(data)
            is Boolean -> JSONBoolean.of(data)
            is Byte -> JSONByte(data)
            is JSONObject -> data
            is JSONArray -> data
            is JSONNumber -> data
            is JSONString -> data
            is Iterable<*> -> {
                val array = JSONArray {
                    for (element in data) {
                        if (element != null) {
                            add(encode(element))
                        } else {
                            addNull()
                        }
                    }
                }
                array
            }

            is Map<*, *> -> {
                val obj = JSONObject {
                    for ((key, value) in data) {
                        if (value != null) {
                            put(key.toString(), encode(value))
                        }
                    }
                }
                obj
            }

            else -> {
                if (data::class.isData) {
                    JSONEncoder.encodeData(data, data::class)
                } else {
                    throw IllegalArgumentException("Unsupported data type: ${data::class}")
                }
            }
        }
    }

    fun encode(name: String, data: Any?, json: JSONObject) {
        if (data == null) {
            json.putNull(name)
            return
        }
        json.apply {
            name set encode(data)
        }
    }

    fun decode(data: JSONObject, key: String, type: KType): Any? {
        if (type.jvmErasure == String::class) {
            return data.getString(key)
        }
        if (type.jvmErasure == Int::class) {
            return data.getInt(key)
        }
        if (type.jvmErasure == Long::class) {
            return data.getLong(key)
        }
        if (type.jvmErasure == Float::class) {
            return data.getFloat(key)
        }
        if (type.jvmErasure == Double::class) {
            return data.getDouble(key)
        }
        if (type.jvmErasure == Boolean::class) {
            return data.getBoolean(key)
        }
        if (type.jvmErasure == Byte::class) {
            return data.getByte(key)
        }
        return JSONDecoder.decode(data, type.jvmErasure)
    }
}
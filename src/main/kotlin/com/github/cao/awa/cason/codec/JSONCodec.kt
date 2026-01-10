package com.github.cao.awa.cason.codec

import com.github.cao.awa.cason.array.JSONArray
import com.github.cao.awa.cason.codec.decoder.JSONDecoder
import com.github.cao.awa.cason.codec.encoder.JSONEncoder
import com.github.cao.awa.cason.obj.JSONObject
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

object JSONCodec {
    inline fun <reified T> decode(data: JSONObject): T {
        return JSONDecoder.decode(data)
    }

    fun encode(data: Any): JSONObject {
        return JSONEncoder.encode(data)
    }

    fun setAdapter(name: String, data: Any?, json: JSONObject) {
        if (data == null) {
            json.putNull(name)
            return
        }
        json.apply {
            when (data) {
                is String -> name set data
                is Int -> name set data
                is Long -> name set data
                is Float -> name set data
                is Double -> name set data
                is Boolean -> name set data
                is Byte -> name set data
                is JSONObject -> name set data
                is JSONArray -> name set data
                else -> setAdapter(
                    name,
                    JSONEncoder.encode(data),
                    json
                )
            }
        }
    }

    fun getAdapter(data: JSONObject, key: String, type: KType): Any? {
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
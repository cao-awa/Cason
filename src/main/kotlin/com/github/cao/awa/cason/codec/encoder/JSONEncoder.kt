package com.github.cao.awa.cason.codec.encoder

import com.github.cao.awa.cason.codec.JSONCodec
import com.github.cao.awa.cason.annotation.Field
import com.github.cao.awa.cason.obj.JSONObject
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

object JSONEncoder {
    inline fun <reified T: Any> encode(data: T): JSONObject {
        val type = data::class
        if (type.isData) {
            return JSONObject {
                data::class.declaredMemberProperties.forEach { property ->
                    if (property.visibility == KVisibility.PUBLIC) {
                        val name = property.findAnnotation<Field>()?.name ?: property.name
                        JSONCodec.setAdapter(name,property.getter.call(data), this)
                    }
                }
            }
        }

        throw IllegalStateException("Cannot encode '${type.qualifiedName}' to JSON because it is not a data class")
    }

    inline fun <reified T: Any> encodeToString(data: T): String {
        return encode(data).toString()
    }
}
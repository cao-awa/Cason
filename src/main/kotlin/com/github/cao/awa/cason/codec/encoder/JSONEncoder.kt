package com.github.cao.awa.cason.codec.encoder

import com.github.cao.awa.cason.codec.JSONCodec
import com.github.cao.awa.cason.annotation.Field
import com.github.cao.awa.cason.obj.JSONObject
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField

object JSONEncoder {
    inline fun <reified T : Any> encode(data: T): JSONObject {
        val type = data::class
        if (type.isData) {
            return JSONObject {
                data::class.declaredMemberProperties.forEach { property ->
                    if (property.visibility == KVisibility.PUBLIC) {
                        val name = property.findAnnotation<Field>()?.name ?: property.name
                        val pending = property.getter.call(data)
                        if (pending != null) {
                            JSONCodec.setAdapter(name, pending, this)
                        } else if (!property.returnType.isMarkedNullable) {
                            throw IllegalStateException("Cannot encode non-nullable property '${property.name}' with null value in '${type.qualifiedName}' to JSON")
                        }
                    }
                }
            }
        }

        throw IllegalStateException("Cannot encode '${type.qualifiedName}' to JSON because it is not a data class")
    }

    inline fun <reified T : Any> encodeToString(data: T): String {
        return encode(data).toString()
    }
}
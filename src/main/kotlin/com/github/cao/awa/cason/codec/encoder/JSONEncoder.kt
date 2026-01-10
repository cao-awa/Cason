package com.github.cao.awa.cason.codec.encoder

import com.github.cao.awa.cason.codec.JSONCodec
import com.github.cao.awa.cason.codec.annotation.Field
import com.github.cao.awa.cason.obj.JSONObject
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

object JSONEncoder {
    fun encode(data: Any): JSONObject {
        val type = data::class
        if (type.isData) {
            val json = JSONObject {
                data::class.declaredMemberProperties.forEach { property ->
                    if (property.visibility == KVisibility.PUBLIC) {
                        val name = property.findAnnotation<Field>()?.name ?: property.name
                        JSONCodec.setAdapter(name,property.getter.call(data), this)
                    }
                }
            }.build()

            return json
        }

        throw IllegalStateException("Cannot encode '${type.qualifiedName}' to JSON because it is not a data class")
    }
}
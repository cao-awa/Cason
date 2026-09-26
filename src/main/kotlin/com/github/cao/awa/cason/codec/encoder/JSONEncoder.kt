package com.github.cao.awa.cason.codec.encoder

import com.github.cao.awa.cason.codec.JSONCodec
import com.github.cao.awa.cason.annotation.Field
import com.github.cao.awa.cason.annotation.Flattened
import com.github.cao.awa.cason.annotation.Nested
import com.github.cao.awa.cason.array.JSONArray
import com.github.cao.awa.cason.obj.JSONObject
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure

@Suppress("unused")
object JSONEncoder {
    inline fun <reified T : Any> encodeData(data: T): JSONObject {
        return encodeData(data, T::class)
    }

    fun encodeData(data: Any, inputType: KClass<*>): JSONObject {
        val type = if (inputType == Any::class) {
            data::class
        } else {
            inputType
        }

        if (!type.isData) {
            throw IllegalStateException(
                "Cannot encode '${type.qualifiedName}' to JSON because it is not a data class"
            )
        }

        return JSONObject {
            type.declaredMemberProperties.forEach { property ->
                if (property.visibility != KVisibility.PUBLIC) return@forEach

                val nested = property.findAnnotation<Nested>()
                val flattened = property.findAnnotation<Flattened>()

                if (nested != null && flattened != null) {
                    error(
                        "Property '${type.simpleName}.${property.name}' cannot be annotated with both @Nested and @Flattened"
                    )
                }

                val jsonName = property.findAnnotation<Field>()?.name ?: property.name
                val value = property.getter.call(data)

                if (value == null) {
                    if (!property.returnType.isMarkedNullable) {
                        error(
                            "Cannot encode non-nullable property '${property.name}' with null value in '${type.qualifiedName}'"
                        )
                    }
                    return@forEach
                }

                if (!value::class.isData) {
                    JSONCodec.encode(jsonName, value, this)
                } else {
                    when {
                        nested != null -> {
                            val encoded = encodeData(value, property.returnType.jvmErasure)
                            jsonName set encoded
                        }

                        flattened != null -> {
                            val encoded = encodeData(value, property.returnType.jvmErasure)
                            encoded.forEach { (k, v) ->
                                JSONCodec.encode(k, v, this)
                            }
                        }

                        else -> {
                            JSONCodec.encode(jsonName, value, this)
                        }
                    }
                }
            }
        }
    }


    inline fun <reified T : Any> encodeToString(data: T): String {
        return encodeData(data).toString()
    }

    fun renderJSON(data: JSONObject): String {
        return data.toString()
    }

    fun renderJSON(data: JSONObject, pretty: Boolean): String {
        return data.toString(pretty, "    ", 0)
    }

    fun renderArray(data: JSONArray): String {
        return data.toString()
    }

    fun renderArray(data: JSONArray, pretty: Boolean): String {
        return data.toString(pretty, "    ", 0)
    }
}
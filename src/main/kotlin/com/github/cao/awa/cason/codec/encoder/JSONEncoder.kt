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
    inline fun <reified T : Any> encode(data: T): JSONObject {
        return encodeAny(data, T::class)
    }

    fun encodeAny(data: Any, inputType: KClass<*>): JSONObject {
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
                    JSONCodec.setAdapter(jsonName, value, this)
                } else {
                    when {
                        nested != null -> {
                            val encoded = encodeAny(value, property.returnType.jvmErasure)
                            jsonName set encoded
                        }

                        flattened != null -> {
                            val encoded = encodeAny(value, property.returnType.jvmErasure)
                            encoded.forEach { (k, v) ->
                                JSONCodec.setAdapter(k, v, this)
                            }
                        }

                        property.returnType.jvmErasure.isData -> {
                            error(
                                "Cannot encode property '${type.simpleName}.${property.name}' because data class properties must be annotated with @Nested or @Flattened"
                            )
                        }

                        else -> {
                            JSONCodec.setAdapter(jsonName, value, this)
                        }
                    }
                }
            }
        }
    }


    inline fun <reified T : Any> encodeToString(data: T): String {
        return encode(data).toString()
    }

    fun encodeJSON(data: JSONObject): String {
        return data.toString()
    }

    fun encodeJSON(data: JSONObject, pretty: Boolean): String {
        return data.toString(pretty, "    ", 0)
    }

    fun encodeArray(data: JSONArray): String {
        return data.toString()
    }

    fun encodeArray(data: JSONArray, pretty: Boolean): String {
        return data.toString(pretty, "    ", 0)
    }
}
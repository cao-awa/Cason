package com.github.cao.awa.cason.codec.decoder

import com.github.cao.awa.cason.codec.JSONCodec
import com.github.cao.awa.cason.annotation.Nested
import com.github.cao.awa.cason.annotation.Field
import com.github.cao.awa.cason.annotation.Flattened
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.serialize.parser.JSONParser
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

@Suppress("unchecked_cast", "unused")
object JSONDecoder {
    inline fun <reified T: Any> decode(data: String, forceType: KClass<*>? = null): T {
        val json = JSONParser.parseObject(data)
        return decode(json, forceType)
    }

    inline fun <reified T : Any> decode(data: JSONObject, forceType: KClass<*>? = null): T {
        val type: KClass<T> = forceType?.let {
            forceType as KClass<T>
        } ?: T::class

        if (type.isData) {
            return decodeDataClass(data, type) as T
        }

        throw IllegalStateException("Cannot decode '${type.qualifiedName}' from JSON because it is not a data class")
    }

    fun decodeDataClass(data: JSONObject, type: KClass<*>): Any {
        val parameters: MutableMap<KParameter, Any?> = mutableMapOf()

        val constructor = type.primaryConstructor ?: error("Data class '${type.qualifiedName}' must have a primary constructor")

        val properties = type.declaredMemberProperties.associateBy {
            it.name
        }

        constructor.parameters.forEach { parameter ->
            val parameterName = parameter.name ?: error("Unnamed constructor parameter in '${type.simpleName}'")

            val property = properties[parameterName]

            val nested = property?.findAnnotation<Nested>()
            val flattened = property?.findAnnotation<Flattened>()

            // Check conflicts, @Nested cannot use with @Flattened.
            if (nested != null && flattened != null) {
                error(
                    "Property '${type.simpleName}.$parameterName' cannot be annotated with both @Nested and @Flattened"
                )
            }

            val jsonName = property?.findAnnotation<Field>()?.name ?: parameterName

            val value = when {
                // Nested.
                nested != null -> {
                    val json = data.getJSON(jsonName)

                    when {
                        json != null ->
                            decodeDataClass(json, parameter.type.jvmErasure)

                        parameter.type.isMarkedNullable ->
                            null

                        else ->
                            error(
                                "Unable to decode property '${type.simpleName}.$parameterName' " +
                                        "because required field '$jsonName' is missing"
                            )
                    }
                }

                // Flattened.
                flattened != null -> {
                    if (!parameter.type.jvmErasure.isData) {
                        error(
                            "@Flattened can only be applied to data class properties: " +
                                    "'${type.simpleName}.$parameterName'"
                        )
                    }

                    decodeDataClass(data, parameter.type.jvmErasure)
                }

                // Default forbid data class.
                parameter.type.jvmErasure.isData -> {
                    error(
                        "Unable to decode property '${type.simpleName}.$parameterName' " +
                                "because data class properties must be annotated with @Nested or @Flattened"
                    )
                }

                // Normal field.
                else -> {
                    JSONCodec.getAdapter(data, jsonName, parameter.type)
                }
            }

            parameters[parameter] = value
        }

        return constructor.callBy(parameters)
    }
}

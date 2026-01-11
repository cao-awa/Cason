package com.github.cao.awa.cason.codec.decoder

import com.github.cao.awa.cason.codec.JSONCodec
import com.github.cao.awa.cason.annotation.Nested
import com.github.cao.awa.cason.annotation.Field
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.serialize.parser.JSONParser
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

@Suppress("unchecked_cast")
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

        val constructor = type.primaryConstructor!!

        val properties: MutableMap<String, KProperty1<*, *>> = type.declaredMemberProperties.let { properties ->
            val map: MutableMap<String, KProperty1<*, *>> = mutableMapOf()
            properties.forEach { property ->
                map[property.name] = property
            }
            map
        }

        constructor.parameters.forEach { parameter ->
            val parameterName = parameter.name
            val name = properties[parameterName]?.let {
                it.findAnnotation<Field>()?.name
            } ?: parameterName ?: error("Unable to decode property '${parameterName}'")

            parameters[parameter] = run {
                var result: Any?
                if (properties[parameterName]?.findAnnotation<Nested>() != null) {
                    val json = data.getJSON(name)

                    result = if (json != null) {
                        decodeDataClass(json, parameter.type.jvmErasure)
                    } else if (!parameter.type.isMarkedNullable) {
                        error("Unable to decode property '${type.simpleName}' because required field '$name' is missing")
                    } else {
                        null
                    }
                } else {
                    if (parameter.type.jvmErasure.isData) {
                        error("Unable to decode property '${parameterName}'($name) because this data class missing @Nested annotation")
                    }
                    result = JSONCodec.getAdapter(data, name, parameter.type)
                }

                result
            }
        }

        return constructor.callBy(parameters)
    }
}

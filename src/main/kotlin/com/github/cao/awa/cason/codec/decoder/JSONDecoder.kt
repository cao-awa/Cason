package com.github.cao.awa.cason.codec.decoder

import com.github.cao.awa.cason.codec.JSONCodec
import com.github.cao.awa.cason.codec.annotation.Nested
import com.github.cao.awa.cason.codec.annotation.Field
import com.github.cao.awa.cason.obj.JSONObject
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

@Suppress("unchecked_cast")
object JSONDecoder {
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
            for (property in properties) {
                map[property.name] = property
            }
            map
        }

        constructor.parameters.forEach { parameter ->
            val name = properties[parameter.name]?.let {
                it.findAnnotation<Field>()?.name
            } ?: parameter.name ?: throw IllegalStateException("Unable to decode property '${parameter.name}'")

            parameters[parameter] = run {
                val isInnerData = properties[parameter.name]?.findAnnotation<Nested>() != null
                if (isInnerData) {
                    decodeDataClass(data.getJSON(name)!!, parameter.type.jvmErasure)
                } else {
                    JSONCodec.getAdapter(data, name, parameter.type)
                }
            }
        }

        return constructor.callBy(parameters)
    }
}

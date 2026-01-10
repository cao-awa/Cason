package com.github.cao.awa.cason.codec.decoder

import com.github.cao.awa.cason.codec.annotation.Data
import com.github.cao.awa.cason.codec.annotation.Mapper
import com.github.cao.awa.cason.obj.JSONObject
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

@Suppress("unchecked_cast")
class JSONDecoder {
    companion object {
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

            val constructor = type.constructors.first()

            constructor.parameters.forEach { parameter ->
                val name = parameter.let {
                    it.findAnnotation<Mapper>()?.name ?: it.name!!
                }

                parameters[parameter] = parameter.let {
                    val isData = it.findAnnotation<Data>() != null
                    if (isData) {
                        decodeDataClass(data.getJSON(name)!!, parameter.type.jvmErasure)
                    } else {
                        getData(data, name, parameter.type)
                    }
                }
            }

            return constructor.callBy(parameters)
        }

        fun getData(data: JSONObject, key: String, type: KType): Any? {
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
            throw NotImplementedError()
        }
    }
}
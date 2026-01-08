@file:Suppress("unused")

package com.github.cao.awa.cason.obj

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.array.JSONArray
import com.github.cao.awa.cason.primary.JSONBoolean
import com.github.cao.awa.cason.primary.JSONNumber
import com.github.cao.awa.cason.primary.JSONString
import com.github.cao.awa.cason.setting.JSONSettings
import com.github.cao.awa.cason.serialize.writer.JSONWriter
import com.github.cao.awa.cason.stream.DataStream
import kotlin.collections.component1
import kotlin.collections.component2

class JSONObject(private val map: LinkedHashMap<String, JSONElement>) : JSONElement {
    private val pendingData: MutableList<DataStream<*>> = ArrayList()

    constructor(body: JSONObject.() -> Unit) : this(LinkedHashMap<String, JSONElement>()) {
        body(this)
    }

    fun array(body: JSONArray.() -> Unit): JSONArray = JSONArray(body)

    fun array(key: String, body: JSONArray.() -> Unit): JSONArray = array(body).also {
        put(key, it)
    }

    fun json(body: JSONObject.() -> Unit): JSONObject = JSONObject(body)

    fun json(key: String, body: JSONObject.() -> Unit): JSONObject = json(body).also {
        put(key, it)
    }

    fun put(key: String, value: JSONObject): JSONObject = putElement(key, value)
    fun put(key: String, value: JSONArray): JSONObject = putElement(key, value)
    fun put(key: String, value: String): JSONObject = putElement(key, JSONString(value))
    fun put(key: String, value: Boolean): JSONObject = putElement(key, JSONBoolean(value))
    fun put(key: String, value: Byte): JSONObject = putElement(key, JSONNumber.ofByte(value))
    fun put(key: String, value: Short): JSONObject = putElement(key, JSONNumber.ofShort(value))
    fun put(key: String, value: Int): JSONObject = putElement(key, JSONNumber.ofInt(value))
    fun put(key: String, value: Long): JSONObject = putElement(key, JSONNumber.ofLong(value))
    fun put(key: String, value: Float): JSONObject = putElement(key, JSONNumber.ofFloat(value))
    fun put(key: String, value: Double): JSONObject = putElement(key, JSONNumber.ofDouble(value))

    infix fun String.set(value: JSONObject): DataStream<JSONObject> = pendingData(value) { put(this, it) }
    infix fun String.set(value: JSONArray): DataStream<JSONArray> = pendingData(value) { put(this, it) }
    infix fun String.set(value: String): DataStream<String> = pendingData(value) { put(this, it) }
    infix fun String.set(value: Boolean): DataStream<Boolean> = pendingData(value) { put(this, it) }
    infix fun String.set(value: Byte): DataStream<Byte> = pendingData(value) { put(this, it) }
    infix fun String.set(value: Short): DataStream<Short> = pendingData(value) { put(this, it) }
    infix fun String.set(value: Int): DataStream<Int> = pendingData(value) { put(this, it) }
    infix fun String.set(value: Long): DataStream<Long> = pendingData(value) { put(this, it) }
    infix fun String.set(value: Float): DataStream<Float> = pendingData(value) { put(this, it) }
    infix fun String.set(value: Double): DataStream<Double> = pendingData(value) { put(this, it) }

    fun string(value: () -> String): String = value()
    fun bool(value: () -> Boolean): Boolean = value()
    fun byte(value: () -> Byte): Byte = value()
    fun short(value: () -> Short): Short = value()
    fun int(value: () -> Int): Int = value()
    fun long(value: () -> Long): Long = value()
    fun float(value: () -> Float): Float = value()
    fun double(value: () -> Double): Double = value()

    private fun putElement(key: String, value: JSONElement): JSONObject {
        this.map[key] = value
        return this
    }

    fun getArray(key: String): JSONArray? = getElement(key) as? JSONArray
    fun getJSON(key: String): JSONObject? = getElement(key) as? JSONObject
    fun getString(key: String): String? = (getElement(key) as? JSONString)?.asString()
    fun getBoolean(key: String): Boolean? = (getElement(key) as? JSONBoolean)?.value
    fun getByte(key: String): Byte? = (getElement(key) as? JSONNumber)?.asByte()
    fun getShort(key: String): Short? = (getElement(key) as? JSONNumber)?.asShort()
    fun getInt(key: String): Int? = (getElement(key) as? JSONNumber)?.asInt()
    fun getLong(key: String): Long? = (getElement(key) as? JSONNumber)?.asLong()
    fun getFloat(key: String): Float? = (getElement(key) as? JSONNumber)?.asFloat()
    fun getDouble(key: String): Double? = (getElement(key) as? JSONNumber)?.asDouble()

    fun getArray(key: String, back: () -> JSONArray): JSONArray = getElement(key) as? JSONArray ?: back()
    fun getJSON(key: String, back: () -> JSONObject): JSONObject = getElement(key) as? JSONObject ?: back()
    fun getString(key: String, back: () -> String): String = (getElement(key) as? JSONString)?.asString() ?: back()
    fun getBoolean(key: String, back: () -> Boolean): Boolean = (getElement(key) as? JSONBoolean)?.value ?: back()
    fun getByte(key: String, back: () -> Byte): Byte = (getElement(key) as? JSONNumber)?.asByte() ?: back()
    fun getShort(key: String, back: () -> Short): Short = (getElement(key) as? JSONNumber)?.asShort() ?: back()
    fun getInt(key: String, back: () -> Int): Int = (getElement(key) as? JSONNumber)?.asInt() ?: back()
    fun getLong(key: String, back: () -> Long): Long = (getElement(key) as? JSONNumber)?.asLong() ?: back()
    fun getFloat(key: String, back: () -> Float): Float = (getElement(key) as? JSONNumber)?.asFloat() ?: back()
    fun getDouble(key: String, back: () -> Double): Double = (getElement(key) as? JSONNumber)?.asDouble() ?: back()

    fun isPresent(key: String): Boolean = this.map.containsKey(key)

    fun computeArray(key: String, back: (JSONArray?) -> JSONArray): JSONObject = put(key, back(getArray(key)))
    fun computeJSON(key: String, back: (JSONObject?) -> JSONObject): JSONObject = put(key, back(getJSON(key)))
    fun computeString(key: String, back: (String?) -> String): JSONObject = put(key, back(getString(key)))
    fun computeBoolean(key: String, back: (Boolean?) -> Boolean): JSONObject = put(key, back(getBoolean(key)))
    fun computeByte(key: String, back: (Byte?) -> Byte): JSONObject = put(key, back(getByte(key)))
    fun computeShort(key: String, back: (Short?) -> Short): JSONObject = put(key, back(getShort(key)))
    fun computeInt(key: String, back: (Int?) -> Int): JSONObject = put(key, back(getInt(key)))
    fun computeLong(key: String, back: (Long?) -> Long): JSONObject = put(key, back(getLong(key)))
    fun computeFloat(key: String, back: (Float?) -> Float): JSONObject = put(key, back(getFloat(key)))
    fun computeDouble(key: String, back: (Double?) -> Double): JSONObject = put(key, back(getDouble(key)))

    private fun <T> pendingData(rawData: T, finalize: (T) -> Unit): DataStream<T> {
        return DataStream(rawData, finalize).also {
            this.pendingData.add(it)
        }
    }

    fun build(): JSONObject {
        this.pendingData.forEach(DataStream<*>::commit)
        this.pendingData.clear()

        this.map.forEach { (_, value) ->
            (value as? JSONObject)?.build()
        }

        return this
    }

    fun path(path: String, body: JSONObject.() -> Unit): JSONObject {
        var base = this
        path.split('.').forEach { key: String ->
            // Use 'getJSON' by base to prevent repeat using path cleaning old data.
            base = base.getJSON(key) ?: JSONObject {
                // Put new JSON object if not present.
                base.put(key, this)
            }
        }

        // Callback given last JSON object.
        body(base)

        return this
    }

    fun forEach(action: (MutableMap.MutableEntry<String, JSONElement>) -> Unit) {
        for (entry in this.map) {
            action(entry)
        }
    }

    private fun getElement(key: String): Any? {
        return this.map[key]
    }

    fun toString(pretty: Boolean, indent: String, depth: Int): String {
        val builder = StringBuilder()
        builder.append('{')
        if (this.map.isEmpty()) {
            builder.append('}')
            return builder.toString()
        }

        if (pretty) {
            builder.append('\n')
        }
        val entries = this.map.entries.toList()
        for (idx in entries.indices) {
            val (k, v) = entries[idx]
            if (pretty) {
                builder.append(indent.repeat(depth + 1))
            }
            builder.append(JSONWriter.renderKey(k))
            if (pretty) {
                builder.append(": ")
            } else {
                builder.append(":")
            }
            JSONWriter.writeValue(builder, v, pretty, indent, depth + 1)
            if (idx != entries.lastIndex) {
                builder.append(',')
            }
            if (pretty) {
                builder.append('\n')
            }
        }
        if (pretty) {
            builder.append(indent.repeat(depth))
        }
        builder.append('}')

        return builder.toString()
    }

    override fun toString(): String = toString(JSONSettings.prettyFormat, "    ", 0)

    override fun isNumber(): Boolean = false
    override fun isObject(): Boolean = true
    override fun isArray(): Boolean = false
    override fun isString(): Boolean = false
    override fun isNull(): Boolean = false
    override fun isBoolean(): Boolean = false
}
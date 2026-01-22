@file:Suppress("unused")

package com.github.cao.awa.cason.array

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.primary.JSONBoolean
import com.github.cao.awa.cason.primary.JSONNull
import com.github.cao.awa.cason.primary.JSONNumber
import com.github.cao.awa.cason.primary.JSONString
import com.github.cao.awa.cason.setting.JSONSettings
import com.github.cao.awa.cason.serialize.writer.JSONWriter

data class JSONArray(val list: ArrayList<JSONElement>) : JSONElement {
    constructor(): this(ArrayList())

    constructor(body: JSONArray.() -> Unit) : this(ArrayList<JSONElement>()) {
        body(this)
    }

    fun add(): JSONArray = add(JSONNull)
    fun add(value: JSONObject): JSONArray = add(value)
    fun add(value: JSONArray): JSONArray = add(value)
    fun add(value: String): JSONArray = add(JSONString(value))
    fun add(value: Boolean): JSONArray = add(JSONBoolean.of(value))
    fun add(value: Byte): JSONArray = add(JSONNumber.ofByte(value))
    fun add(value: Short): JSONArray = add(JSONNumber.ofShort(value))
    fun add(value: Int): JSONArray = add(JSONNumber.ofInt(value))
    fun add(value: Long): JSONArray = add(JSONNumber.ofLong(value))
    fun add(value: Float): JSONArray = add(JSONNumber.ofFloat(value))
    fun add(value: Double): JSONArray = add(JSONNumber.ofDouble(value))

    operator fun String.unaryPlus(): JSONArray = add(this)
    operator fun Boolean.unaryPlus(): JSONArray = add(this)

    fun getArray(index: Int): JSONArray? = getElement(index) as? JSONArray
    fun getJSON(index: Int): JSONObject? = getElement(index) as? JSONObject
    fun getString(index: Int): String? = (getElement(index) as? JSONString)?.asString()
    fun getBoolean(index: Int): Boolean? = (getElement(index) as? JSONBoolean)?.value
    fun getByte(index: Int): Byte? = (getElement(index) as? JSONNumber)?.asByte()
    fun getShort(index: Int): Short? = (getElement(index) as? JSONNumber)?.asShort()
    fun getInt(index: Int): Int? = (getElement(index) as? JSONNumber)?.asInt()
    fun getLong(index: Int): Long? = (getElement(index) as? JSONNumber)?.asLong()
    fun getFloat(index: Int): Float? = (getElement(index) as? JSONNumber)?.asFloat()
    fun getDouble(index: Int): Double? = (getElement(index) as? JSONNumber)?.asDouble()

    fun getArray(index: Int, back: () -> JSONArray): JSONArray = getElement(index) as? JSONArray ?: back()
    fun getJSON(index: Int, back: () -> JSONObject): JSONObject = getElement(index) as? JSONObject ?: back()
    fun getString(index: Int, back: () -> String): String = (getElement(index) as? JSONString)?.asString() ?: back()
    fun getBoolean(index: Int, back: () -> Boolean): Boolean = (getElement(index) as? JSONBoolean)?.value ?: back()
    fun getByte(index: Int, back: () -> Byte): Byte = (getElement(index) as? JSONNumber)?.asByte() ?: back()
    fun getShort(index: Int, back: () -> Short): Short = (getElement(index) as? JSONNumber)?.asShort() ?: back()
    fun getInt(index: Int, back: () -> Int): Int = (getElement(index) as? JSONNumber)?.asInt() ?: back()
    fun getLong(index: Int, back: () -> Long): Long = (getElement(index) as? JSONNumber)?.asLong() ?: back()
    fun getFloat(index: Int, back: () -> Float): Float = (getElement(index) as? JSONNumber)?.asFloat() ?: back()
    fun getDouble(index: Int, back: () -> Double): Double = (getElement(index) as? JSONNumber)?.asDouble() ?: back()

    fun computeArray(index: Int, back: (JSONArray?) -> JSONArray): JSONArray = set(index, back(getArray(index)))
    fun computeJSON(index: Int, back: (JSONObject?) -> JSONObject): JSONArray = set(index, back(getJSON(index)))
    fun computeString(index: Int, back: (String?) -> String): JSONArray = set(index, back(getString(index)))
    fun computeBoolean(index: Int, back: (Boolean?) -> Boolean): JSONArray = set(index, back(getBoolean(index)))
    fun computeByte(index: Int, back: (Byte?) -> Byte): JSONArray = set(index, back(getByte(index)))
    fun computeShort(index: Int, back: (Short?) -> Short): JSONArray = set(index, back(getShort(index)))
    fun computeInt(index: Int, back: (Int?) -> Int): JSONArray = set(index, back(getInt(index)))
    fun computeLong(index: Int, back: (Long?) -> Long): JSONArray = set(index, back(getLong(index)))
    fun computeFloat(index: Int, back: (Float?) -> Float): JSONArray = set(index, back(getFloat(index)))
    fun computeDouble(index: Int, back: (Double?) -> Double): JSONArray = set(index, back(getDouble(index)))

    fun set(index: Int, value: JSONArray) = setElement(index, value)
    fun set(index: Int, value: JSONObject) = setElement(index, value)
    fun set(index: Int, value: String) = setElement(index, JSONString(value))
    fun set(index: Int, value: Boolean) = setElement(index, JSONBoolean.of(value))
    fun set(index: Int, value: Byte) = setElement(index, JSONNumber.ofByte(value))
    fun set(index: Int, value: Short) = setElement(index, JSONNumber.ofShort(value))
    fun set(index: Int, value: Int) = setElement(index, JSONNumber.ofInt(value))
    fun set(index: Int, value: Long) = setElement(index, JSONNumber.ofLong(value))
    fun set(index: Int, value: Float) = setElement(index, JSONNumber.ofFloat(value))
    fun set(index: Int, value: Double) = setElement(index, JSONNumber.ofDouble(value))

    fun isEmpty(): Boolean = this.list.isEmpty()

    fun size(): Int = this.list.size

    private fun setElement(index: Int, value: JSONElement): JSONArray {
        this.list[index] = value
        return this
    }

    private fun getElement(index: Int): JSONElement? {
        return this.list[index].let {
            if (it is JSONNull) {
                null
            } else {
                it
            }
        }
    }

    fun add(element: JSONElement): JSONArray {
        this.list.add(element)
        return this
    }

    fun toString(pretty: Boolean, indent: String, depth: Int): String {
        val builder = StringBuilder()
        builder.append('[')
        if (this.list.isEmpty()) {
            builder.append(']');
            return builder.toString()
        }

        if (pretty) {
            builder.append('\n')
        }
        for (idx in this.list.indices) {
            if (pretty) {
                builder.append(indent.repeat(depth + 1))
            }
            JSONWriter.writeValue(builder, this.list[idx], pretty, indent, depth + 1)
            if (idx != this.list.lastIndex) {
                builder.append(',')
            }
            if (pretty) {
                builder.append('\n')
            }
        }
        if (pretty) {
            builder.append(indent.repeat(depth))
        }
        builder.append(']')

        return builder.toString()
    }

    override fun toString(): String = toString(JSONSettings.prettyFormat, "    ", 0)

    override fun isNumber(): Boolean = false
    override fun isObject(): Boolean = false
    override fun isArray(): Boolean = true
    override fun isString(): Boolean = false
    override fun isNull(): Boolean = false
    override fun isBoolean(): Boolean = false
}
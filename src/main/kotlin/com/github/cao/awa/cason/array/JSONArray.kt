package com.github.cao.awa.cason.array

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.parser.JSONParser.writeValue
import com.github.cao.awa.cason.primary.JSONBoolean
import com.github.cao.awa.cason.primary.JSONNull
import com.github.cao.awa.cason.primary.JSONNumber
import com.github.cao.awa.cason.primary.JSONString
import com.github.cao.awa.cason.setting.JSONSettings

data class JSONArray(val list: ArrayList<JSONElement>) : JSONElement {
    constructor(body: JSONArray.() -> Unit) : this(ArrayList<JSONElement>()) {
        body(this)
    }

    fun add(): JSONArray = add(JSONNull)
    fun add(value: JSONObject): JSONArray = add(value)
    fun add(value: JSONArray): JSONArray = add(value)
    fun add(value: String): JSONArray = add(JSONString(value))
    fun add(value: Boolean): JSONArray = add(JSONBoolean(value))
    fun add(value: Byte): JSONArray = add(JSONNumber.ofByte(value))
    fun add(value: Short): JSONArray = add(JSONNumber.ofShort(value))
    fun add(value: Int): JSONArray = add(JSONNumber.ofInt(value))
    fun add(value: Long): JSONArray = add(JSONNumber.ofLong(value))
    fun add(value: Float): JSONArray = add(JSONNumber.ofFloat(value))
    fun add(value: Double): JSONArray = add(JSONNumber.ofDouble(value))

    operator fun String.unaryPlus(): JSONArray = add(this)
    operator fun Boolean.unaryPlus(): JSONArray = add(this)

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
            writeValue(builder, this.list[idx], pretty, indent, depth + 1)
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
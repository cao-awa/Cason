package com.github.cao.awa.caoson.arr

import com.github.cao.awa.caoson.JSONElement
import com.github.cao.awa.caoson.parser.JSONParser.renderKey
import com.github.cao.awa.caoson.parser.JSONParser.writeValue
import com.github.cao.awa.caoson.setting.JSONSettings
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.get
import kotlin.text.iterator

data class JSONArray(val list: ArrayList<JSONElement>) : JSONElement {
    fun add(element: JSONElement) {
        this.list.add(element)
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
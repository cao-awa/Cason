package com.github.cao.awa.cason.obj

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.parser.JSONParser.renderKey
import com.github.cao.awa.cason.parser.JSONParser.writeValue
import com.github.cao.awa.cason.setting.JSONSettings
import kotlin.collections.component1
import kotlin.collections.component2

class JSONObject(private val map: LinkedHashMap<String, JSONElement>): JSONElement {
    fun put(key: String, value: JSONElement): JSONObject {
        this.map[key] = value
        return this
    }

    fun get(key: String): Any? {
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
            builder.append(renderKey(k))
            if (pretty) {
                builder.append(": ")
            } else {
                builder.append(":")
            }
            writeValue(builder, v, pretty, indent, depth + 1)
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
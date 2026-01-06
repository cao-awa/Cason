package com.github.cao.awa.cason.writer

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.array.JSONArray
import com.github.cao.awa.cason.obj.JSONObject
import com.github.cao.awa.cason.primary.JSONBoolean
import com.github.cao.awa.cason.primary.JSONNull
import com.github.cao.awa.cason.primary.JSONNumber
import com.github.cao.awa.cason.primary.JSONString
import com.github.cao.awa.cason.serialize.JSONSerializeVersion
import com.github.cao.awa.cason.setting.JSONSettings
import com.github.cao.awa.cason.util.CasonUtil

object JSONWriter {
    fun stringify(
        value: JSONElement,
        pretty: Boolean = false,
        indent: String = "  "
    ): String {
        val sb = StringBuilder()
        writeValue(sb, value, pretty, indent, 0)
        return sb.toString()
    }

    fun writeValue(builder: StringBuilder, element: JSONElement, pretty: Boolean, indent: String, depth: Int) {
        when (element) {
            is JSONNull -> builder.append("null")
            is JSONBoolean -> {
                if (element.value) {
                    builder.append("true")
                } else {
                    builder.append("false")
                }
            }

            is JSONString -> builder.append(renderString(element.asString()))
            is JSONNumber -> builder.append(element.toString())
            is JSONArray -> writeArray(builder, element, pretty, indent, depth)
            is JSONObject -> writeObject(builder, element, pretty, indent, depth)
        }
    }

    private fun writeArray(builder: StringBuilder, array: JSONArray, pretty: Boolean, indent: String, depth: Int) {
        builder.append(array.toString(pretty, indent, depth))
    }

    private fun writeObject(builder: StringBuilder, obj: JSONObject, pretty: Boolean, indent: String, depth: Int) {
        builder.append(obj.toString(pretty, indent, depth))
    }

    fun renderKey(key: String): String {
        return if (isSafeUnquotedKey(key) && JSONSettings.serializeVersion == JSONSerializeVersion.JSON5) {
            key
        } else {
            renderString(key)
        }
    }

    private fun isSafeUnquotedKey(key: String): Boolean {
        if (key.isEmpty()) {
            return false
        }
        val first = key[0]
        if (CasonUtil.isIdStart(first)) {
            for (i in 1 until key.length) {
                if (!CasonUtil.isIdPart(key[i])) {
                    return false
                }
            }
            return true
        }
        return false
    }

    private fun renderString(s: String): String {
        val quote = if (JSONSettings.preferSingleQuote) {
            '\''
        } else {
            '"'
        }
        val builder = StringBuilder()
        builder.append(quote)
        for (ch in s) {
            when (ch) {
                '\\' -> builder.append("\\\\")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                '\u000B' -> builder.append("\\v")
                '\'' -> {
                    if (quote == '\'') {
                        builder.append("\\'")
                    } else {
                        builder.append('\'')
                    }
                }

                '"' -> {
                    if (quote == '"') {
                        builder.append("\\\"")
                    } else {
                        builder.append('"')
                    }
                }

                '\u2028' -> builder.append("\\u2028")
                '\u2029' -> builder.append("\\u2029")
                else -> {
                    if (ch.code < 0x20) {
                        builder.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        builder.append(ch)
                    }
                }
            }
        }
        builder.append(quote)
        return builder.toString()
    }
}
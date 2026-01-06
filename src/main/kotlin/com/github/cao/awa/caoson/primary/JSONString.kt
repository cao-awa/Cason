package com.github.cao.awa.caoson.primary

import com.github.cao.awa.caoson.JSONElement

data class JSONString(private val value: String) : JSONElement {
    fun asString() = this.value

    override fun isNumber(): Boolean = false
    override fun isObject(): Boolean = false
    override fun isArray(): Boolean = false
    override fun isString(): Boolean = true
    override fun isNull(): Boolean = false
    override fun isBoolean(): Boolean = false
}

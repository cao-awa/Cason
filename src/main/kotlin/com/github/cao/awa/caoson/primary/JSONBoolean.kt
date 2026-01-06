package com.github.cao.awa.caoson.primary

import com.github.cao.awa.caoson.JSONElement

data class JSONBoolean(val value: Boolean) : JSONElement {
    override fun isNumber(): Boolean = false
    override fun isObject(): Boolean = false
    override fun isArray(): Boolean = false
    override fun isString(): Boolean = false
    override fun isNull(): Boolean = false
    override fun isBoolean(): Boolean = true
}
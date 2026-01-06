package com.github.cao.awa.cason.primary

import com.github.cao.awa.cason.JSONElement

data class JSONBoolean(val value: Boolean) : JSONElement {
    override fun isNumber(): Boolean = false
    override fun isObject(): Boolean = false
    override fun isArray(): Boolean = false
    override fun isString(): Boolean = false
    override fun isNull(): Boolean = false
    override fun isBoolean(): Boolean = true
}
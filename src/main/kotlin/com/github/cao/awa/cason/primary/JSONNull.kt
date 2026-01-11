package com.github.cao.awa.cason.primary

import com.github.cao.awa.cason.JSONElement

data object JSONNull : JSONElement {
    override fun isNumber(): Boolean = false
    override fun isObject(): Boolean = false
    override fun isArray(): Boolean = false
    override fun isString(): Boolean = false
    override fun isNull(): Boolean = true
    override fun isBoolean(): Boolean = false

    override fun toString(): String = "null"
}
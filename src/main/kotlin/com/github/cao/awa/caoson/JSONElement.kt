package com.github.cao.awa.caoson

interface JSONElement {
    fun isNumber(): Boolean

    fun isObject(): Boolean

    fun isArray(): Boolean

    fun isString(): Boolean

    fun isNull(): Boolean

    fun isBoolean(): Boolean
}
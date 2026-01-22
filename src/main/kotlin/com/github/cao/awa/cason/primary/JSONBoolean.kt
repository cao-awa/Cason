package com.github.cao.awa.cason.primary

import com.github.cao.awa.cason.JSONElement

data class JSONBoolean(val value: Boolean) : JSONElement {
    companion object {
        val TRUE = JSONBoolean(true)
        val FALSE = JSONBoolean(false)

        fun of(bool: Boolean): JSONBoolean {
            if (bool) {
                return TRUE
            } else {
                return FALSE
            }
        }
    }

    override fun isNumber(): Boolean = false
    override fun isObject(): Boolean = false
    override fun isArray(): Boolean = false
    override fun isString(): Boolean = false
    override fun isNull(): Boolean = false
    override fun isBoolean(): Boolean = true

    override fun toString(): String = this.value.toString()
}
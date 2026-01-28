package com.github.cao.awa.cason.primary

import com.github.cao.awa.cason.JSONElement

class JSONBoolean : JSONElement {
    companion object {
        val TRUE = JSONBoolean()
        val FALSE = JSONBoolean()

        fun of(bool: Boolean): JSONBoolean {
            return if (bool) {
                TRUE
            } else {
                FALSE
            }
        }
    }

    val value: Boolean
        get() {
            return this == TRUE
        }

    override fun isNumber(): Boolean = false
    override fun isObject(): Boolean = false
    override fun isArray(): Boolean = false
    override fun isString(): Boolean = false
    override fun isNull(): Boolean = false
    override fun isBoolean(): Boolean = true

    override fun toString(): String {
        if (this == TRUE) {
            return "true"
        }
        return "false"
    }
}
package com.github.cao.awa.cason.primary.number

import com.github.cao.awa.cason.primary.JSONNumber

class JSONDouble(val value: Double): JSONNumber() {
    override fun asByte(): Byte = this.value.toInt().toByte()

    override fun asShort(): Short = this.value.toInt().toShort()

    override fun asInt(): Int = this.value.toInt()

    override fun asLong(): Long = this.value.toLong()

    override fun asFloat(): Float = this.value.toFloat()

    override fun asDouble(): Double = this.value

    override fun toString(): String {
        return this.value.toString()
    }
}
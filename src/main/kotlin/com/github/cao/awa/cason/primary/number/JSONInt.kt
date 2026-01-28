package com.github.cao.awa.cason.primary.number

import com.github.cao.awa.cason.primary.JSONNumber

class JSONInt(val value: Int): JSONNumber() {
    override fun asByte(): Byte = this.value.toByte()

    override fun asShort(): Short = this.value.toShort()

    override fun asInt(): Int = this.value

    override fun asLong(): Long = this.value.toLong()

    override fun asFloat(): Float = this.value.toFloat()

    override fun asDouble(): Double = this.value.toDouble()

    override fun toString(): String {
        return this.value.toString()
    }
}
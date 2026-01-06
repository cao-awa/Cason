package com.github.cao.awa.caoson.primary

import com.github.cao.awa.caoson.JSONElement
import com.github.cao.awa.caoson.number.CasonNumber
import java.math.BigDecimal

data class JSONNumber(val number: CasonNumber) : JSONElement {
    fun toByte(): Byte = number.toByte()

    fun asShort(): Short = this.number.toShort()

    fun asInt(): Int = this.number.toInt()

    fun asLong(): Long = this.number.toLong()

    fun asFloat(): Float = this.number.toFloat()

    fun asDouble(): Double = this.number.toDouble()

    override fun toString(): String {
        return when (number.kind) {
            CasonNumber.Kind.NAN -> "NaN"
            CasonNumber.Kind.POS_INF -> "Infinity"
            CasonNumber.Kind.NEG_INF -> "-Infinity"
            CasonNumber.Kind.FINITE -> {
                val value = number.value ?: BigDecimal.ZERO
                // normalize: 0E-10 -> 0 .
                val normalize = try {
                    value.stripTrailingZeros()
                } catch (_: Exception) {
                    value
                }
                val txt = try {
                    normalize.toPlainString()
                } catch (_: Exception) {
                    normalize.toString()
                }
                // BigDecimal may output "-0" sometimes.
                if (txt == "-0") "0" else txt
            }
        }
    }

    override fun isNumber(): Boolean = true
    override fun isObject(): Boolean = false
    override fun isArray(): Boolean = false
    override fun isString(): Boolean = false
    override fun isNull(): Boolean = false
    override fun isBoolean(): Boolean = false
}
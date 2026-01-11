package com.github.cao.awa.cason.primary

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.primary.number.CasonNumber
import java.math.BigDecimal

data class JSONNumber(val number: CasonNumber) : JSONElement {
    companion object {
        fun ofByte(value: Byte): JSONNumber = JSONNumber(CasonNumber.ofByte(value))
        fun ofShort(value: Short): JSONNumber = JSONNumber(CasonNumber.ofShort(value))
        fun ofInt(value: Int): JSONNumber = JSONNumber(CasonNumber.ofInt(value))
        fun ofLong(value: Long): JSONNumber = JSONNumber(CasonNumber.ofLong(value))
        fun ofFloat(value: Float): JSONNumber = JSONNumber(CasonNumber.ofFloat(value))
        fun ofDouble(value: Double): JSONNumber = JSONNumber(CasonNumber.ofDouble(value))
    }

    fun asByte(): Byte = number.toByte()
    fun asShort(): Short = this.number.toShort()
    fun asInt(): Int = this.number.toInt()
    fun asLong(): Long = this.number.toLong()
    fun asFloat(): Float = this.number.toFloat()
    fun asDouble(): Double = this.number.toDouble()

    fun isFinite(): Boolean = this.number.kind == CasonNumber.Kind.FINITE

    fun isNaN(): Boolean = this.number.kind == CasonNumber.Kind.NAN
    fun isPositiveInfinity(): Boolean = this.number.kind == CasonNumber.Kind.POS_INF
    fun isNegativeInfinity(): Boolean = this.number.kind == CasonNumber.Kind.NEG_INF
    fun isInfinity(): Boolean = isPositiveInfinity() || isNegativeInfinity()

    override fun toString(): String {
        return when (this.number.kind) {
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
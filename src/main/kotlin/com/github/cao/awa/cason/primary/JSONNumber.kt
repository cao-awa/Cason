package com.github.cao.awa.cason.primary

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.primary.number.JSONBigDecimal
import com.github.cao.awa.cason.primary.number.JSONByte
import com.github.cao.awa.cason.primary.number.JSONDouble
import com.github.cao.awa.cason.primary.number.JSONFloat
import com.github.cao.awa.cason.primary.number.JSONInt
import com.github.cao.awa.cason.primary.number.JSONLong
import com.github.cao.awa.cason.primary.number.JSONShort
import com.github.cao.awa.cason.primary.number.infinity.JSONNegativeInfinity
import com.github.cao.awa.cason.primary.number.infinity.JSONPositiveInfinity
import com.github.cao.awa.cason.primary.number.nan.JSONNaN
import java.math.BigDecimal

abstract class JSONNumber : JSONElement {
    companion object {
        val POSITIVE_INFINITY: JSONPositiveInfinity = JSONPositiveInfinity
        val NEGATIVE_INFINITY: JSONNegativeInfinity = JSONNegativeInfinity
        val NAN: JSONNaN = JSONNaN

        fun ofByte(value: Byte): JSONByte = JSONByte(value)
        fun ofShort(value: Short): JSONShort = JSONShort(value)
        fun ofInt(value: Int): JSONInt = JSONInt(value)
        fun ofLong(value: Long): JSONLong = JSONLong(value)
        fun ofFloat(value: Float): JSONFloat = JSONFloat(value)
        fun ofDouble(value: Double): JSONDouble = JSONDouble(value)
        fun ofBig(value: BigDecimal): JSONBigDecimal = JSONBigDecimal(value)

        fun adapter(bigDecimal: BigDecimal): JSONNumber {
            val precision = bigDecimal.precision()
            // Decimal
            if (bigDecimal.stripTrailingZeros().scale() > 0) {
                return ofDouble(bigDecimal.toDouble())
            }
            // Integer.
            if (precision < 19) {
                if (precision < 10) {
                    return ofInt(bigDecimal.toInt())
                }
                return ofLong(bigDecimal.longValueExact())
            }
            return ofBig(bigDecimal)
        }
    }

    abstract fun asByte(): Byte
    abstract fun asShort(): Short
    abstract fun asInt(): Int
    abstract fun asLong(): Long
    abstract fun asFloat(): Float
    abstract fun asDouble(): Double

    fun isNaN(): Boolean = this == NAN
    fun isPositiveInfinity(): Boolean = this == POSITIVE_INFINITY
    fun isNegativeInfinity(): Boolean = this == NEGATIVE_INFINITY
    fun isInfinity(): Boolean = isPositiveInfinity() || isNegativeInfinity()

    abstract override fun toString(): String

    override fun isNumber(): Boolean = true
    override fun isObject(): Boolean = false
    override fun isArray(): Boolean = false
    override fun isString(): Boolean = false
    override fun isNull(): Boolean = false
    override fun isBoolean(): Boolean = false
}
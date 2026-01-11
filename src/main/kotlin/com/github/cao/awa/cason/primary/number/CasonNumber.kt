package com.github.cao.awa.cason.primary.number

import java.math.BigDecimal

data class CasonNumber(
    val kind: Kind,
    val value: BigDecimal? = null
) {
    enum class Kind(val kindName: String) {
        FINITE("finite"),
        NAN("NaN"),
        POS_INF("positive infinity"),
        NEG_INF("negative infinity"),
    }

    companion object {
        fun finite(number: BigDecimal) = CasonNumber(Kind.FINITE, number)
        fun nan() = CasonNumber(Kind.NAN, null)
        fun posInf() = CasonNumber(Kind.POS_INF, null)
        fun negInf() = CasonNumber(Kind.NEG_INF, null)

        fun ofByte(value: Byte): CasonNumber = CasonNumber(Kind.FINITE, BigDecimal.valueOf(value.toLong()))
        fun ofShort(value: Short): CasonNumber = CasonNumber(Kind.FINITE, BigDecimal.valueOf(value.toLong()))
        fun ofInt(value: Int): CasonNumber = CasonNumber(Kind.FINITE, BigDecimal.valueOf(value.toLong()))
        fun ofLong(value: Long): CasonNumber = CasonNumber(Kind.FINITE, BigDecimal.valueOf(value))
        fun ofFloat(value: Float): CasonNumber = CasonNumber(Kind.FINITE, BigDecimal.valueOf(value.toDouble()))
        fun ofDouble(value: Double): CasonNumber = CasonNumber(Kind.FINITE, BigDecimal.valueOf(value))
    }

    private fun getNumber(): BigDecimal {
        return this.value ?: throw IllegalArgumentException("Value is ${this.kind.kindName}")
    }

    fun toByte(): Byte = getNumber().toByte()

    fun toShort(): Short = getNumber().toShort()

    fun toInt(): Int = getNumber().toInt()

    fun toLong(): Long = getNumber().toLong()

    fun toFloat(): Float = getNumber().toFloat()

    fun toDouble(): Double = getNumber().toDouble()
}
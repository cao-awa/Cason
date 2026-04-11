package com.github.cao.awa.cason.primary.number.nan

import com.github.cao.awa.cason.primary.JSONNumber
import java.math.BigDecimal

object JSONNaN: JSONNumber() {
    override fun asBigDecimal(): BigDecimal {
        error("NaN cannot be a real number")
    }

    override fun asByte(): Byte {
        error("NaN cannot be a real number")
    }

    override fun asShort(): Short {
        error("NaN cannot be a real number")
    }

    override fun asInt(): Int {
        error("NaN cannot be a real number")
    }

    override fun asLong(): Long {
        error("NaN cannot be a real number")
    }

    override fun asFloat(): Float {
        error("NaN cannot be a real number")
    }

    override fun asDouble(): Double {
        error("NaN cannot be a real number")
    }

    override fun toString(): String {
        return "NaN"
    }
}
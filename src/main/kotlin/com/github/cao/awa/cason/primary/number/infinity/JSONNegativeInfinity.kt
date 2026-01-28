package com.github.cao.awa.cason.primary.number.infinity

import com.github.cao.awa.cason.primary.JSONNumber

object JSONNegativeInfinity: JSONNumber() {
    override fun asByte(): Byte {
        error("Infinity cannot be a real number")
    }

    override fun asShort(): Short {
        error("Infinity cannot be a real number")
    }

    override fun asInt(): Int {
        error("Infinity cannot be a real number")
    }

    override fun asLong(): Long {
        error("Infinity cannot be a real number")
    }

    override fun asFloat(): Float {
        error("Infinity cannot be a real number")
    }

    override fun asDouble(): Double {
        error("Infinity cannot be a real number")
    }

    override fun toString(): String {
        return "-Infinity"
    }
}
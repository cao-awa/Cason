package com.github.cao.awa.cason.stream.validate

import com.github.cao.awa.cason.stream.DataStream

data class ValidateResult<T>(val value: T, val success: Boolean, val reason: String = "*") {
    companion object {
        val SUCCESS = ValidateResult(Any(),true)
    }
}
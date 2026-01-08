package com.github.cao.awa.cason.stream.validate

data class Validation<T>(val data: T) {
    fun success(): ValidateResult<T> = ValidateResult(this.data, true)
    fun successIf(success: Boolean, failureMessage: String = "*"): ValidateResult<T> =
        successIf({ success }, failureMessage)

    fun successIf(predicate: () -> Boolean, failureMessage: String = "*"): ValidateResult<T> {
        return if (predicate()) {
            success()
        } else {
            failure(failureMessage)
        }
    }

    fun failure(): ValidateResult<T> = ValidateResult(this.data, false)
    fun failure(reason: String): ValidateResult<T> = ValidateResult(this.data, false, reason)
    fun failureIf(failure: Boolean, reason: String): ValidateResult<T> = failureIf({ failure }, reason)
    fun failureIf(predicate: () -> Boolean, reason: String): ValidateResult<T> {
        return if (predicate()) {
            failure(reason)
        } else {
            success()
        }
    }

    fun result(predicate: () -> Boolean): ValidateResult<T> = ValidateResult(this.data, predicate())
    fun result(result: Boolean): ValidateResult<T> = ValidateResult(this.data, result)
}
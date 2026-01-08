package com.github.cao.awa.cason.stream

class DataStream<T>(private val rawData: T, private val finalize: (T) -> Unit) {
    private var success: Boolean = true
    private var data: T = this.rawData

    infix fun validate(body: T.() -> Boolean): DataStream<T> {
        this.success = this.success and body(this.data)

        return this
    }

    infix fun map(body: T.(T) -> T): DataStream<T> {
        this.data = body(this.data, this.data)

        return this
    }

    infix fun mapIfValidated(body: T.(T) -> T): DataStream<T> {
        this.data = body(this.data, this.data)

        return this
    }

    fun commit() {
        if (this.success) {
            this.finalize(this.data)
        }
    }
}
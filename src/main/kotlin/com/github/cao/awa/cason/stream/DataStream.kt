package com.github.cao.awa.cason.stream

class DataStream<T>(private val rawData: T, private val finalize: (T) -> Unit) {
    private var validated: Boolean = true
    private var data: T = this.rawData

    infix fun validate(body: T.() -> Boolean): DataStream<T> {
        this.validated = this.validated and body(this.data)

        return this
    }

    infix fun map(body: T.(T) -> T): DataStream<T> {
        this.data = body(this.data, this.data)

        return this
    }

    infix fun mapIfValidated(body: T.(T) -> T): DataStream<T> {
        if (this.validated) {
            this.data = body(this.data, this.data)
        }

        return this
    }

    infix fun ifValidated(body: T.(T) -> Unit): DataStream<T> {
        if (this.validated) {
            body(this.data, this.data)
        }

        return this
    }

    infix fun ifUnvalidated(body: T.(T) -> Unit): DataStream<T> {
        if (!this.validated) {
            body(this.data, this.data)
        }

        return this
    }

    fun commit() {
        if (this.validated) {
            this.finalize(this.data)
        }
    }
}
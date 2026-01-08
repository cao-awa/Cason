package com.github.cao.awa.cason.stream

import com.github.cao.awa.cason.stream.validate.ValidateResult
import com.github.cao.awa.cason.stream.validate.Validation
import java.util.LinkedList

class DataStream<T>(private val rawData: T, private val finalize: (T) -> Unit) {
    private var validation: ValidateResult<T> = ValidateResult(this.rawData, true)
    private var data: T = this.rawData

    private var validators: LinkedList<((T) -> Unit)> = LinkedList()
    private var mappers: LinkedList<(T) -> Unit> = LinkedList()
    private var posters: LinkedList<(T) -> Unit> = LinkedList()
    private var defaultProvider: ((T) -> Unit)? = null

    infix fun validate(body: Validation<T>.() -> ValidateResult<T>): DataStream<T> {
        val validators: LinkedList<(T) -> Unit> = LinkedList(this.validators)
        validators.add {
            if (this.validation.success) {
                this.validation = body(Validation(it))
            }
        }
        this.validators = validators

        return this
    }

    infix fun ifValidated(body: ValidateResult<T>.() -> Unit): DataStream<T> {
        val posters: LinkedList<(T) -> Unit> = LinkedList(this.posters)
        posters.add {
            if (this.validation.success) {
                body(this.validation)
            }
        }
        this.posters = posters

        return this
    }

    infix fun ifUnvalidated(body: ValidateResult<T>.() -> Unit): DataStream<T> {
        val posters: LinkedList<(T) -> Unit> = LinkedList(this.posters)
        posters.add {
            if (!this.validation.success) {
                body(this.validation)
            }
        }
        this.posters = posters

        return this
    }

    infix fun map(body: T.(T) -> T): DataStream<T> {
        val mappers: LinkedList<(T) -> Unit> = LinkedList(this.mappers)
        mappers.add {
            this.data = body(this.data, this.data)
            this.data
        }
        this.mappers = mappers

        return this
    }

    infix fun mapIfValidated(body: T.(T) -> T): DataStream<T> {
        val mappers: LinkedList<(T) -> Unit> = LinkedList(this.mappers)
        mappers.add {
            if (this.validation.success) {
                map(body)
            }
        }
        this.mappers = mappers

        return this
    }

    infix fun defaultSet(body: T.(T) -> T): DataStream<T> {
        this.defaultProvider = {
            this.data = body(this.data, this.data)
        }

        return this
    }

    fun commit() {
        this.validators.forEach {
            it(this.data)
        }
        if (this.validation.success) {
            this.mappers.forEach {
                it(this.data)
            }
        }
        this.posters.forEach {
            it(this.data)
        }
        if (this.validation.success) {
            this.finalize(this.data)
        } else {
            if (this.defaultProvider != null) {
                this.defaultProvider!!.invoke(this.data)
                this.finalize(this.data)
            }
        }
    }
}
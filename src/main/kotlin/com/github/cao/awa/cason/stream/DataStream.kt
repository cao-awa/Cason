package com.github.cao.awa.cason.stream

import java.util.LinkedList

class DataStream<T>(private val rawData: T, private val finalize: (T) -> Unit) {
    private var validated: Boolean = true
    private var data: T = this.rawData

    private var validator: ((T) -> Unit)? = null
    private var invalidator: ((T) -> Unit)? = null
    private var mappers: LinkedList<(T) -> Unit> = LinkedList()
    private var posters: LinkedList<(T) -> Unit> = LinkedList()
    private var defaultProvider: ((T) -> Unit)? = null

    infix fun validate(body: T.() -> Boolean): DataStream<T> {
        if (this.validator != null) {
            throw IllegalStateException("Duplicated validator")
        }
        this.validator = {
            this.validated = this.validated and body(it)
        }

        return this
    }

    infix fun onInvalidate(body: T.() -> Unit): DataStream<T> {
        if (this.validator != null) {
            throw IllegalStateException("Duplicated invalidator")
        }
        this.invalidator = {
            if (this.validated) {
                body(it)
            }
        }

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
            if (this.validated) {
                map(body)
            }
        }
        this.mappers = mappers

        return this
    }

    infix fun ifValidated(body: T.(T) -> Unit): DataStream<T> {
        val posters: LinkedList<(T) -> Unit> = LinkedList(this.posters)
        posters.add {
            if (this.validated) {
                body(this.data, this.data)
            }
        }
        this.posters = posters

        return this
    }

    infix fun ifUnvalidated(body: T.(T) -> Unit): DataStream<T> {
        val posters: LinkedList<(T) -> Unit> = LinkedList(this.posters)
        posters.add {
            if (!this.validated) {
                body(this.data, this.data)
            }
        }
        this.posters = posters

        return this
    }

    infix fun defaultSet(body: T.(T) -> T): DataStream<T> {
        this.defaultProvider = {
            this.data = body(this.data, this.data)
        }

        return this
    }

    fun commit() {
        this.validator?.invoke(this.data)
        if (!this.validated) {
            this.invalidator?.invoke(this.data)

        }
        if (this.validated) {
            this.mappers.forEach {
                it(this.data)
            }
        }
        this.posters.forEach {
            it(this.data)
        }
        if (this.validated) {
            this.finalize(this.data)
        } else{
            if (this.defaultProvider != null) {
                this.defaultProvider!!.invoke(this.data)
                this.finalize(this.data)
            }
        }
    }
}
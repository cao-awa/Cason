@file:Suppress("unused")

package com.github.cao.awa.cason.obj

import com.github.cao.awa.cason.JSONElement
import com.github.cao.awa.cason.annotation.CallOnlyInternal
import com.github.cao.awa.cason.array.JSONArray
import com.github.cao.awa.cason.codec.decoder.JSONDecoder
import com.github.cao.awa.cason.codec.encoder.JSONEncoder
import com.github.cao.awa.cason.primary.JSONBoolean
import com.github.cao.awa.cason.primary.JSONNull
import com.github.cao.awa.cason.primary.JSONNumber
import com.github.cao.awa.cason.primary.JSONString
import com.github.cao.awa.cason.setting.JSONSettings
import com.github.cao.awa.cason.serialize.writer.JSONWriter
import com.github.cao.awa.cason.stream.DataStream
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * A mutable JSON object representation backed by a LinkedHashMap.
 *
 * This class implements [JSONElement] and provides a builder-style API to
 * construct nested JSON structures programmatically. It also supports
 * deferred/pending data via [DataStream], getters with optional fallbacks,
 * and helpers for encoding/decoding nested Kotlin objects.
 *
 * @property map the internal insertion-ordered map that stores key -> JSONElement
 *
 * @author cao_awa
 *
 * @since 1.0.0
 */
class JSONObject(private val map: HashMap<String, JSONElement>) : JSONElement {
    private val pendingData: MutableList<DataStream<*>> = ArrayList()

    /**
     * Construct an empty JSONObject
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    constructor() : this(HashMap<String, JSONElement>())

    /**
     * Construct an empty JSONObject and run [body] to populate it.
     *
     * Example: JSONObject {
     *     "key" set "value"
     * }
     *
     * @param body builder lambda that receives this JSONObject as receiver
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    constructor(body: JSONObject.() -> Unit) : this(HashMap<String, JSONElement>()) {
        instruct(body)
    }

    /**
     * Run the given builder [body] on this object and complete any pending
     * data streams registered during the build.
     *
     * Returns this JSONObject for fluent chaining.
     *
     * @param body builder lambda to execute on this object
     * @return this JSONObject after executing [body] and completing pending streams
     *
     * @author cao_awa
     *
     * @since 1.0.10
     */
    fun instruct(body: JSONObject.() -> Unit): JSONObject {
        body(this)
        completePending()
        return this
    }

    /**
     * Create a new [JSONArray] by executing [body].
     * The returned array is not automatically inserted into this object.
     *
     * @param body builder lambda that populates the array
     * @return the created [JSONArray]
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun array(body: JSONArray.() -> Unit): JSONArray = JSONArray(body)

    /**
     * Create and insert a new [JSONArray] under [key] by executing [body].
     *
     * @param key the key to insert the created array under
     * @param body builder lambda that populates the array
     * @return the created and inserted [JSONArray]
     *
     * @author cao_awa
     */
    fun array(key: String, body: JSONArray.() -> Unit): JSONArray = array(body).also {
        put(key, it)
    }

    /**
     * Create a new [JSONObject] by executing [body]. The returned object is
     * not automatically inserted into this object.
     *
     * @param body builder lambda that populates the created object
     * @return the created [JSONObject]
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun json(body: JSONObject.() -> Unit): JSONObject = JSONObject(body)

    /**
     * Create and insert a new [JSONObject] under [key] by executing [body].
     *
     * @param key the key to insert the created object under
     * @param body builder lambda that populates the created object
     * @return the created and inserted [JSONObject]
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun json(key: String, body: JSONObject.() -> Unit): JSONObject = json(body).also {
        put(key, it)
    }

    /**
     * Helper for encoding a nested Kotlin object and inserting it under [key].
     * The generic type [T] is encoded to a [JSONElement] using [JSONEncoder].
     *
     * @param T reified type to encode
     * @param key the key to insert the encoded value under
     * @param back a supplier lambda producing an object of type [T]
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.11
     */
    inline fun <reified T: Any> nested(key: String, back: () -> T): JSONObject = putNested(key, back())

    /**
     * Put a JSON null at [key].
     *
     * @param key the key to insert the null value at
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun putNull(key: String) = putElement(key, JSONNull)

    /**
     * Put a nested [JSONNumber] value at [key].
     *
     * @param key the key to insert the number at
     * @param value the [JSONNumber] value to insert
     * @return this JSONNumber for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun put(key: String, value: JSONNumber): JSONObject = putElement(key, value)

    /**
     * Put a nested [JSONString] value at [key].
     *
     * @param key the key to insert the string at
     * @param value the [JSONString] value to insert
     * @return this JSONString for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun put(key: String, value: JSONString): JSONObject = putElement(key, value)

    /**
     * Put a nested [JSONObject] value at [key].
     *
     * @param key the key to insert the object at
     * @param value the [JSONObject] value to insert
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun put(key: String, value: JSONObject): JSONObject = putElement(key, value)

    /**
     * Put a nested [JSONArray] value at [key].
     *
     * @param key the key to insert the array at
     * @param value the [JSONArray] value to insert
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun put(key: String, value: JSONArray): JSONObject = putElement(key, value)

    /**
     * Put a string value at [key].
     *
     * @param key the key to insert the string at
     * @param value the string value to insert
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun put(key: String, value: String): JSONObject = putElement(key, JSONString(value))

    /**
     * Put a boolean value at [key].
     *
     * @param key the key to insert the boolean at
     * @param value the boolean value to insert
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun put(key: String, value: Boolean): JSONObject = putElement(key, JSONBoolean(value))

    /**
     * Put a byte numeric value at [key].
     *
     * @param key the key to insert the number at
     * @param value the byte value to insert
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun put(key: String, value: Byte): JSONObject = putElement(key, JSONNumber.ofByte(value))

    /**
     * Put a short numeric value at [key].
     *
     * @param key the key to insert the number at
     * @param value the short value to insert
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun put(key: String, value: Short): JSONObject = putElement(key, JSONNumber.ofShort(value))

    /**
     * Put an int numeric value at [key].
     *
     * @param key the key to insert the number at
     * @param value the int value to insert
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun put(key: String, value: Int): JSONObject = putElement(key, JSONNumber.ofInt(value))

    /**
     * Put a long numeric value at [key].
     *
     * @param key the key to insert the number at
     * @param value the long value to insert
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun put(key: String, value: Long): JSONObject = putElement(key, JSONNumber.ofLong(value))

    /**
     * Put a float numeric value at [key].
     *
     * @param key the key to insert the number at
     * @param value the float value to insert
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun put(key: String, value: Float): JSONObject = putElement(key, JSONNumber.ofFloat(value))

    /**
     * Put a double numeric value at [key].
     *
     * @param key the key to insert the number at
     * @param value the double value to insert
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun put(key: String, value: Double): JSONObject = putElement(key, JSONNumber.ofDouble(value))

    /**
     * Encode an arbitrary Kotlin object [value] to a [JSONElement] and put it at
     * [key]. Returns this JSONObject for chaining.
     *
     * @param key the key to insert the encoded value at
     * @param value the Kotlin data class object to encode and insert
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.11
     */
    inline fun <reified T: Any> putNested(key: String, value: T): JSONObject {
        JSONEncoder.encode(value).also {
            put(key, it)
        }
        return this
    }

    /**
     * Register a pending JSONNumber under the receiver key.
     *
     * @param value the JSONNumber value to insert
     * @return a [DataStream] which will commit the value when finalized
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    infix fun String.set(value: JSONNumber): DataStream<JSONNumber> = pendingData(value) { put(this, it) }

    /**
     * Register a pending JSONString under the receiver key.
     *
     * @param value the JSONString value to insert
     * @return a [DataStream] which will commit the value when finalized
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    infix fun String.set(value: JSONString): DataStream<JSONString> = pendingData(value) { put(this, it) }
    
    /**
     * Register a pending JSONObject under the receiver key.
     *
     * @param value the JSONObject value to insert
     * @return a [DataStream] which will commit the value when finalized
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    infix fun String.set(value: JSONObject): DataStream<JSONObject> = pendingData(value) { put(this, it) }

    /**
     * Register a pending JSONArray under the receiver key.
     *
     * @param value the JSONArray value to insert
     * @return a [DataStream] which will commit the value when finalized
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    infix fun String.set(value: JSONArray): DataStream<JSONArray> = pendingData(value) { put(this, it) }

    /**
     * Register a pending String under the receiver key.
     *
     * @param value the String value to insert
     * @return a [DataStream] which will commit the value when finalized
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    infix fun String.set(value: String): DataStream<String> = pendingData(value) { put(this, it) }

    /**
     * Register a pending Boolean under the receiver key.
     *
     * @param value the Boolean value to insert
     * @return a [DataStream] which will commit the value when finalized
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    infix fun String.set(value: Boolean): DataStream<Boolean> = pendingData(value) { put(this, it) }

    /**
     * Register a pending Byte under the receiver key.
     *
     * @param value the Byte value to insert
     * @return a [DataStream] which will commit the value when finalized
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    infix fun String.set(value: Byte): DataStream<Byte> = pendingData(value) { put(this, it) }

    /**
     *  Register a pending Short under the receiver key.
     *
     * @param value the Short value to insert
     * @return a [DataStream] which will commit the value when finalized
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    infix fun String.set(value: Short): DataStream<Short> = pendingData(value) { put(this, it) }

    /**
     *  Register a pending Int under the receiver key.
     *
     * @param value the Int value to insert
     * @return a [DataStream] which will commit the value when finalized
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    infix fun String.set(value: Int): DataStream<Int> = pendingData(value) { put(this, it) }

    /**
     * Register a pending Long under the receiver key.
     *
     * @param value the Long value to insert
     * @return a [DataStream] which will commit the value when finalized
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    infix fun String.set(value: Long): DataStream<Long> = pendingData(value) { put(this, it) }

    /**
     * Register a pending Float under the receiver key.
     *
     * @param value the Float value to insert
     * @return a [DataStream] which will commit the value when finalized
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    infix fun String.set(value: Float): DataStream<Float> = pendingData(value) { put(this, it) }

    /**
     * Register a pending Double under the receiver key.
     *
     * @param value the Double value to insert
     * @return a [DataStream] which will commit the value when finalized
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    infix fun String.set(value: Double): DataStream<Double> = pendingData(value) { put(this, it) }

    /**
     * Register a pending encoded nested value under the string receiver.
     * Example: "nested" nested SomeData()
     *
     * @param value the Kotlin data class object to encode and insert
     * @return a [DataStream] which will commit the encoded value when finalized
     *
     * @author cao_awa
     *
     * @since 1.0.11
     */
    inline infix fun <reified T: Any> String.nested(value: T): DataStream<T> = pendingData(value) { putNested(this, it) }

    /**
     * Evaluate and return a String from [value]. Useful in DSL defaults.
     *
     * @param value the supplier lambda producing a String
     *
     * @return the produced String
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun string(value: () -> String): String = value()

    /**
     * Evaluate and return a Boolean from [value]. Useful in DSL defaults.
     *
     * @param value the supplier lambda producing a Boolean
     *
     * @return the produced Boolean
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun bool(value: () -> Boolean): Boolean = value()

    /**
     * Evaluate and return a Byte from [value]. Useful in DSL defaults.
     *
     * @param value the supplier lambda producing a Byte
     *
     * @return the produced Byte
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun byte(value: () -> Byte): Byte = value()

    /**
     * Evaluate and return a Short from [value]. Useful in DSL defaults.
     *
     * @param value the supplier lambda producing a Short
     *
     * @return the produced Short
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun short(value: () -> Short): Short = value()

    /**
     * Evaluate and return an Int from [value]. Useful in DSL defaults.
     *
     * @param value the supplier lambda producing an Int
     *
     * @return the produced Int
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun int(value: () -> Int): Int = value()

    /**
     * Evaluate and return a Long from [value]. Useful in DSL defaults.
     *
     * @param value the supplier lambda producing a Long
     *
     * @return the produced Long
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun long(value: () -> Long): Long = value()

    /**
     * Evaluate and return a Float from [value]. Useful in DSL defaults.
     *
     * @param value the supplier lambda producing a Float
     *
     * @return the produced Float
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun float(value: () -> Float): Float = value()

    /**
     * Evaluate and return a Double from [value]. Useful in DSL defaults.
     *
     * @param value the supplier lambda producing a Double
     *
     * @return the produced Double
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun double(value: () -> Double): Double = value()

    /**
     * Evaluate and return a nested object from [value]. Useful in DSL defaults.
     *
     * @param value the supplier lambda producing an object of type [T]
     *
     * @return the produced object of type [T]
     *
     * @author cao_awa
     *
     * @since 1.0.11
     */
    inline fun <reified T : Any> nested(value: () -> T): T = value()

    /**
     * Internal helper to insert a [JSONElement] into the backing map.
     * Returns this JSONObject for chaining.
     *
     * @param key the key to insert under
     * @param value the JSONElement to insert
     * @return this JSONObject
     */
    private fun putElement(key: String, value: JSONElement): JSONObject {
        this.map[key] = value
        return this
    }

    /**
     * Get an array value stored under [key], or null if absent or mismatched.
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun getArray(key: String): JSONArray? = getElement(key) as? JSONArray

    /** Get a JSON object stored under [key], or null if absent or mismatched. */
    fun getJSON(key: String): JSONObject? = getElement(key) as? JSONObject

    /** Get a string stored under [key], or null if absent or mismatched. */
    fun getString(key: String): String? = (getElement(key) as? JSONString)?.asString()

    /** Get a boolean stored under [key], or null if absent or mismatched. */
    fun getBoolean(key: String): Boolean? = (getElement(key) as? JSONBoolean)?.value

    /**
     * Numeric getters that attempt to coerce to the target type or return null.
     *
     * Each method returns null when the stored value is missing or not a number.
     *
     * @param key the key to look up the numeric value
     * @return the coerced numeric value or null
     *
     * @since 1.0.0
     */
    fun getByte(key: String): Byte? = (getElement(key) as? JSONNumber)?.asByte()
    fun getShort(key: String): Short? = (getElement(key) as? JSONNumber)?.asShort()
    fun getInt(key: String): Int? = (getElement(key) as? JSONNumber)?.asInt()
    fun getLong(key: String): Long? = (getElement(key) as? JSONNumber)?.asLong()
    fun getFloat(key: String): Float? = (getElement(key) as? JSONNumber)?.asFloat()
    fun getDouble(key: String): Double? = (getElement(key) as? JSONNumber)?.asDouble()

    /**
     * Decode a nested Kotlin object of type [T] from the JSON object stored
     * under [key]. Returns null when the key is not present or not an object.
     *
     * @param key the key to look up the nested object
     *
     * @return decoded instance of [T] or null when missing/mismatched
     *
     * @author cao_awa
     *
     * @since 1.0.11
     */
    inline fun <reified T : Any> getNested(key: String): T? {
        return getJSON(key)?.let {
            JSONDecoder.decode<T>(it)
        }
    }

    /**
     * Get an array under [key] or return the result of [back] when missing/mismatched.
     *
     * @param key the key to look up
     * @param back fallback supplier invoked when value is absent or wrong type
     *
     * @return the found JSONArray or the fallback result
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun getArray(key: String, back: () -> JSONArray): JSONArray = getElement(key) as? JSONArray ?: back()

    /**
     * Get a JSON object under [key] or return the result of [back] when missing/mismatched.
     *
     * @param key the key to look up
     * @param back fallback supplier invoked when value is absent or wrong type
     *
     * @return the found JSONObject or the fallback result
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun getJSON(key: String, back: () -> JSONObject): JSONObject = getElement(key) as? JSONObject ?: back()

    /**
     * Get a string under [key] or return the result of [back] when missing/mismatched.
     *
     * @param key the key to look up
     * @param back fallback supplier invoked when value is absent or wrong type
     *
     * @return the found String or the fallback result
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun getString(key: String, back: () -> String): String = (getElement(key) as? JSONString)?.asString() ?: back()

    /**
     * Get a boolean under [key] or return the result of [back] when missing/mismatched.
     *
     * @param key the key to look up
     * @param back fallback supplier invoked when value is absent or wrong type
     *
     * @return the found Boolean or the fallback result
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun getBoolean(key: String, back: () -> Boolean): Boolean = (getElement(key) as? JSONBoolean)?.value ?: back()

    /**
     * Get a byte under [key] or return the result of [back] when missing/mismatched.
     *
     * @param key the key to look up
     * @param back fallback supplier invoked when value is absent or wrong type
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun getByte(key: String, back: () -> Byte): Byte = (getElement(key) as? JSONNumber)?.asByte() ?: back()

    /**
     * Get a short under [key] or return the result of [back] when missing/mismatched.
     *
     * @param key the key to look up
     * @param back fallback supplier invoked when value is absent or wrong type
     *
     * @return the found Short or the fallback result
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun getShort(key: String, back: () -> Short): Short = (getElement(key) as? JSONNumber)?.asShort() ?: back()

    /**
     * Get an int under [key] or return the result of [back] when missing/mismatched.
     *
     * @param key the key to look up
     * @param back fallback supplier invoked when value is absent or wrong type
     *
     * @return the found Int or the fallback result
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun getInt(key: String, back: () -> Int): Int = (getElement(key) as? JSONNumber)?.asInt() ?: back()

    /**
     * Get a long under [key] or return the result of [back] when missing/mismatched.
     *
     * @param key the key to look up
     * @param back fallback supplier invoked when value is absent or wrong type
     *
     * @return the found Long or the fallback result
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun getLong(key: String, back: () -> Long): Long = (getElement(key) as? JSONNumber)?.asLong() ?: back()

    /**
     * Get a float under [key] or return the result of [back] when missing/mismatched.
     *
     * @param key the key to look up
     * @param back fallback supplier invoked when value is absent or wrong type
     *
     * @return the found Float or the fallback result
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun getFloat(key: String, back: () -> Float): Float = (getElement(key) as? JSONNumber)?.asFloat() ?: back()

    /**
     * Get a double under [key] or return the result of [back] when missing/mismatched.
     *
     * @param key the key to look up
     * @param back fallback supplier invoked when value is absent or wrong type
     *
     * @return the found Double or the fallback result
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun getDouble(key: String, back: () -> Double): Double = (getElement(key) as? JSONNumber)?.asDouble() ?: back()

    /**
     * Decode a nested Kotlin object of type [T] stored under [key] or return
     * the provided fallback value from [back] when missing.
     *
     * @param key the key to look up the nested object
     * @param back the fallback lambda to invoke when the key is missing
     *
     * @return decoded instance of [T] or the fallback result
     *
     * @since 1.0.11
     */
    inline fun <reified T : Any> getNested(key: String, back: () -> T): T {
        return getJSON(key)?.let {
            JSONDecoder.decode<T>(it)
        } ?: back()
    }

    /**
     * Returns true if the given key exists in this object.
     *
     * @param key the key to check for presence
     *
     * @return true if the key exists, false otherwise
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun isPresent(key: String): Boolean = this.map.containsKey(key)

    /**
     * Compute and set an array at [key] using [back] with the current value (or null).
     *
     * @param key the key to compute/insert
     * @param back computation function receiving current JSONArray? and returning JSONArray
     *
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun computeArray(key: String, back: (JSONArray?) -> JSONArray): JSONObject = put(key, back(getArray(key)))

    /**
     * Compute and set a JSONObject at [key] using [back] with the current value (or null).
     *
     * @param key the key to compute/insert
     * @param back computation function receiving current JSONObject? and returning JSONObject
     *
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun computeJSON(key: String, back: (JSONObject?) -> JSONObject): JSONObject = put(key, back(getJSON(key)))

    /**
     * Compute and set a String at [key] using [back] with the current value (or null).
     *
     * @param key the key to compute/insert
     * @param back computation function receiving current String? and returning String
     *
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun computeString(key: String, back: (String?) -> String): JSONObject = put(key, back(getString(key)))

    /**
     * Compute and set a Boolean at [key] using [back] with the current value (or null).
     *
     * @param key the key to compute/insert
     * @param back computation function receiving current Boolean? and returning Boolean
     *
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun computeBoolean(key: String, back: (Boolean?) -> Boolean): JSONObject = put(key, back(getBoolean(key)))

    /**
     * Compute and set a Byte at [key] using [back] with the current value (or null).
     *
     * @param key the key to compute/insert
     * @param back computation function receiving current Byte? and returning Byte
     *
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun computeByte(key: String, back: (Byte?) -> Byte): JSONObject = put(key, back(getByte(key)))

    /**
     * Compute and set a Short at [key] using [back] with the current value (or null).
     *
     * @param key the key to compute/insert
     * @param back computation function receiving current Short? and returning Short
     *
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun computeShort(key: String, back: (Short?) -> Short): JSONObject = put(key, back(getShort(key)))

    /**
     * Compute and set an Int at [key] using [back] with the current value (or null).
     *
     * @param key the key to compute/insert
     * @param back computation function receiving current Int? and returning Int
     *
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun computeInt(key: String, back: (Int?) -> Int): JSONObject = put(key, back(getInt(key)))

    /**
     * Compute and set a Long at [key] using [back] with the current value (or null).
     *
     * @param key the key to compute/insert
     * @param back computation function receiving current Long? and returning Long
     *
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun computeLong(key: String, back: (Long?) -> Long): JSONObject = put(key, back(getLong(key)))

    /**
     * Compute and set a Float at [key] using [back] with the current value (or null).
     *
     * @param key the key to compute/insert
     * @param back computation function receiving current Float? and returning Float
     *
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun computeFloat(key: String, back: (Float?) -> Float): JSONObject = put(key, back(getFloat(key)))

    /**
     * Compute and set a Double at [key] using [back] with the current value (or null).
     *
     * @param key the key to compute/insert
     * @param back computation function receiving current Double? and returning Double
     *
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun computeDouble(key: String, back: (Double?) -> Double): JSONObject = put(key, back(getDouble(key)))

    /**
     * Compute and set a nested value of type [T] at [key] using [back] with the current value (or null).
     *
     * @param key the key to compute/insert
     * @param back computation function receiving current T? and returning T
     *
     * @return this JSONObject for chaining
     *
     * @author cao_awa
     *
     * @since 1.0.11
     */
    inline fun <reified T : Any> computeNested(key: String, back: (T?) -> T) {
        putNested(key) {
            back(getNested(key))
        }
    }

    /**
     * Create a pending [DataStream] from [rawData] and register a finalizer
     * [finalize] which will be executed when the stream is committed. Marked
     * with [CallOnlyInternal] because it is intended for internal DSL usage.
     *
     * @param rawData the raw data to wrap in a pending stream
     * @param finalize the finalizer lambda to run when committing the stream
     *
     * @return the created [DataStream] instance
     *
     * @since 1.0.0
     */
    @CallOnlyInternal
    fun <T> pendingData(rawData: T, finalize: (T) -> Unit): DataStream<T> {
        return DataStream(rawData, finalize).also {
            this.pendingData.add(it)
        }
    }

    /**
     * Commit all pending data streams and recursively complete child objects.
     * Returns this JSONObject for chaining.
     *
     * @return this JSONObject after committing and clearing pending streams
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun completePending(): JSONObject {
        this.pendingData.forEach(DataStream<*>::commit)
        this.pendingData.clear()

        this.map.forEach { (_, value) ->
            (value as? JSONObject)?.completePending()
        }

        return this
    }

    /**
     * Navigate or create a nested path of dot-separated keys and run [body]
     * against the final JSONObject.
     *
     * Example: obj.path("a.b.c") { "x" set 1 }
     *
     * @param path the dot-separated key path to navigate/create
     * @param body the builder lambda to run against the final JSONObject
     * @return this JSONObject
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun path(path: String, body: JSONObject.() -> Unit): JSONObject {
        var base = this
        path.split('.').forEach { key: String ->
            // Use 'getJSON' by base to prevent repeat using path cleaning old data.
            base = base.getJSON(key) ?: JSONObject {
                // Put new JSON object if not present.
                base.put(key, this)
            }
        }

        // Callback given last JSON object.
        body(base)

        return this
    }

    /**
     * Iterate over each entry in the backing map and invoke [action].
     *
     * @param action consumer invoked with each mutable map entry
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun forEach(action: (MutableMap.MutableEntry<String, JSONElement>) -> Unit) {
        for (entry in this.map) {
            action(entry)
        }
    }

    /**
     * Internal helper which ensures pending data is completed before returning
     * the raw element value stored at [key].
     *
     * @param key the key to look up element
     * @return the raw stored value (maybe null or a JSONElement)
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    private fun getElement(key: String): JSONElement {
        completePending()
        return this.map[key] ?: JSONNull
    }

    /**
     * Serialize this object to a String with configurable pretty printing.
     *
     * @param pretty whether to format with newlines/indentation
     * @param indent the indent string to repeat per level (e.g. "    ")
     * @param depth current depth used during recursive writing
     *
     * @return the serialized JSON string
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun toString(pretty: Boolean, indent: String, depth: Int): String {
        val builder = StringBuilder()
        builder.append('{')
        if (this.map.isEmpty()) {
            builder.append('}')
            return builder.toString()
        }

        if (pretty) {
            builder.append('\n')
        }
        val entries = this.map.entries.toList()
        for (idx in entries.indices) {
            val (k, v) = entries[idx]
            if (pretty) {
                builder.append(indent.repeat(depth + 1))
            }
            builder.append(JSONWriter.renderKey(k))
            if (pretty) {
                builder.append(": ")
            } else {
                builder.append(":")
            }
            JSONWriter.writeValue(builder, v, pretty, indent, depth + 1)
            if (idx != entries.lastIndex) {
                builder.append(',')
            }
            if (pretty) {
                builder.append('\n')
            }
        }
        if (pretty) {
            builder.append(indent.repeat(depth))
        }
        builder.append('}')

        return builder.toString()
    }

    /**
     * Return the serialized JSON string using current [JSONSettings].
     *
     * This override delegates to [toString] with configured pretty-print
     * settings and a default indent.
     *
     * @return JSON string representation of this object
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    override fun toString(): String = toString(JSONSettings.prettyFormat, "    ", 0)

    /**
     * Returns true when this element represents a JSON number. Always false
     * for JSONObject.
     *
     * @return false
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    override fun isNumber(): Boolean = false

    /**
     * Returns true when this element represents a JSON object. Always true
     * for JSONObject.
     *
     * @return true
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    override fun isObject(): Boolean = true

    /**
     * Returns true when this element represents a JSON array. Always false
     * for JSONObject.
     *
     * @return false
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    override fun isArray(): Boolean = false

    /**
     * Returns true when this element represents a JSON string. Always false
     * for JSONObject.
     *
     * @return false
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    override fun isString(): Boolean = false

    /**
     * Returns true when this element represents the JSON null value. Always
     * false for JSONObject.
     *
     * @return false
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    override fun isNull(): Boolean = false

    /**
     * Returns true when this element represents a JSON boolean. Always false
     * for JSONObject.
     *
     * @return false
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    override fun isBoolean(): Boolean = false
}
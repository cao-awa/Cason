package com.github.cao.awa.cason

/**
 * Represents a JSON value.
 *
 * A JSON value can be one of: object, array, string, number, boolean, or null.
 * Implementations of this interface represent a single JSON element and
 * provide type-query methods to determine which kind of JSON value is held.
 *
 * @author cao_awa
 *
 * @since 1.0.0
 */
interface JSONElement {
    /**
     * Returns true when this element represents a JSON number.
     *
     * @return true if this element is a JSON number, false otherwise
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun isNumber(): Boolean

    /**
     * Returns true when this element represents a JSON object.
     *
     * @return true if this element is a JSON object, false otherwise
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun isObject(): Boolean

    /**
     * Returns true when this element represents a JSON array.
     *
     * @return true if this element is a JSON array, false otherwise
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun isArray(): Boolean

    /**
     * Returns true when this element represents a JSON string.
     *
     * @return true if this element is a JSON string, false otherwise
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun isString(): Boolean

    /**
     * Returns true when this element represents the JSON null value.
     *
     * @return true if this element is JSON null, false otherwise
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun isNull(): Boolean

    /**
     * Returns true when this element represents a JSON boolean.
     *
     * @return true if this element is a JSON boolean, false otherwise
     *
     * @author cao_awa
     *
     * @since 1.0.0
     */
    fun isBoolean(): Boolean
}
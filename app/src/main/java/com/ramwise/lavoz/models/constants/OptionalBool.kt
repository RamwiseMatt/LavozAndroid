package com.ramwise.lavoz.models.constants

import com.ramwise.lavoz.utils.exceptions.NativeException


/** The purpose of this constant is to provide a mechanism that allows easy RxJava filtering
 * of optional booleans. Working with Boolean? and null values is not desirable, so use this
 * constant instead.
 *
 * An example usage is to have an RxVariable with an initial value of OptionalBool.UNDEFINED, which
 * is then filtered out of the matching Observable. The remaining values are then mapped to actual
 * Boolean values.
 */
object OptionalBool {
    @JvmStatic val TRUE: Int = 0
    @JvmStatic val FALSE: Int = 1
    @JvmStatic val UNDEFINED: Int = 2

    @JvmStatic fun toBoolean(v: Int) : Boolean {
        if (v == TRUE) return true
        else if (v == FALSE) return false

        throw NativeException("OptionalBool.UNDEFINED cannot be converted to a Boolean.")
    }

    @JvmStatic fun fromBoolean(v: Boolean) : Int {
        if (v) return TRUE
        else return FALSE
    }
}


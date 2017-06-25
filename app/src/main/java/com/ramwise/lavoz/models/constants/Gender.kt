package com.ramwise.lavoz.models.constants

import com.ramwise.lavoz.utils.exceptions.NativeException

object Gender {
    @JvmStatic val MALE: Int = 0
    @JvmStatic val FEMALE: Int = 1
    @JvmStatic val UNDEFINED: Int = 2

    /** Returns a Gender constant by its associated string value. */
    @JvmStatic fun byString(v: String) : Int {
        when (v) {
            "male" -> return MALE
            "female" -> return FEMALE
        }
        return UNDEFINED
    }

    /** Returns a String based on the given Gender constant */
    @JvmStatic fun toString(v: Int) : String {
        when (v) {
            MALE -> return "male"
            FEMALE -> return "female"
        }

        throw NativeException("Passed an invalid Gender constant.")
    }
}

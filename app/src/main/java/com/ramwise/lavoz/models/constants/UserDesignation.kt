package com.ramwise.lavoz.models.constants

import com.ramwise.lavoz.utils.exceptions.NativeException


object UserDesignation {
    @JvmStatic val INDIVIDUAL: Int = 0
    @JvmStatic val JOURNALIST: Int = 1
    @JvmStatic val POLITICAL_PARTY: Int = 2
    @JvmStatic val CORPORATION: Int = 3
    @JvmStatic val FOUNDATION: Int = 4
    @JvmStatic val LAVOZ: Int = 5
    @JvmStatic val UNDEFINED: Int = 6

    /** Returns a UserDesignation constant by its associated string value. */
    @JvmStatic fun byString(v: String) : Int {
        when (v) {
            "individual" -> return INDIVIDUAL
            "journalist" -> return JOURNALIST
            "political_party" -> return POLITICAL_PARTY
            "corporation" -> return CORPORATION
            "foundation" -> return FOUNDATION
            "lavoz" -> return LAVOZ
        }
        return UNDEFINED
    }

    /** Returns a String based on the given UserDesignation constant */
    @JvmStatic fun toString(v: Int) : String {
        when (v) {
            INDIVIDUAL -> return "individual"
            JOURNALIST -> return "journalist"
            POLITICAL_PARTY -> return "political_party"
            CORPORATION -> return "corporation"
            FOUNDATION -> return "foundation"
            LAVOZ -> return "lavoz"
        }

        throw NativeException("Passed an invalid UserDesignation constant.")
    }
}

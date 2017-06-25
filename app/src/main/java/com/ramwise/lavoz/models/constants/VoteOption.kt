package com.ramwise.lavoz.models.constants

import com.ramwise.lavoz.utils.exceptions.NativeException


object VoteOption {
    @JvmStatic val AGREE: Int = 0
    @JvmStatic val DISAGREE: Int = 1
    @JvmStatic val CLEAR: Int = 2
    @JvmStatic val NONE: Int = 3
    @JvmStatic val UNDEFINED: Int = 4

    /** Returns a VoteOption constant by its associated string value. */
    @JvmStatic fun byString(v: String) : Int {
        when (v) {
            "agree" -> return AGREE
            "disagree" -> return DISAGREE
            "clear" -> return CLEAR
            "none" -> return NONE
        }
        return UNDEFINED
    }

    /** Returns a String based on the given VoteOption constant */
    @JvmStatic fun toString(v: Int) : String {
        when (v) {
            AGREE -> return "agree"
            DISAGREE -> return "disagree"
            CLEAR -> return "clear"
            NONE -> return "none"
        }

        throw NativeException("Passed an invalid VoteOption constant.")
    }
}

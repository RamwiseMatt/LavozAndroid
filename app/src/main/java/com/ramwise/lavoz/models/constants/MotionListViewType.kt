package com.ramwise.lavoz.models.constants

import com.ramwise.lavoz.utils.exceptions.NativeException

object MotionListViewType {
    @JvmStatic val RECENT: Int = 0
    @JvmStatic val VOTED_ON: Int = 1
    @JvmStatic val MAJORITY: Int = 2
    @JvmStatic val MINORITY: Int = 3
    @JvmStatic val MOTIVATED: Int = 4
    @JvmStatic val UNDEFINED: Int = 5

    /** Returns a MotionListViewType constant by its associated string value. */
    @JvmStatic fun byString(v: String) : Int {
        when (v) {
            "recent" -> return RECENT
            "voted_on" -> return VOTED_ON
            "majority" -> return MAJORITY
            "minority" -> return MINORITY
            "motivated" -> return MOTIVATED
        }
        return UNDEFINED
    }

    /** Returns a String based on the given MotionListViewType constant */
    @JvmStatic fun toString(v: Int) : String {
        when (v) {
            RECENT -> return "recent"
            VOTED_ON -> return "voted_on"
            MAJORITY -> return "majority"
            MINORITY -> return "minority"
            MOTIVATED -> return "motivated"
        }

        throw NativeException("Passed an invalid MotionListViewType constant.")
    }
}

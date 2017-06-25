package com.ramwise.lavoz.models.constants


object MotionStatus {
    @JvmStatic val UPCOMING: Int = 0
    @JvmStatic val OPEN: Int = 1
    @JvmStatic val TALLYING: Int = 2
    @JvmStatic val CONCLUDED: Int = 3
    @JvmStatic val INACTIVE: Int = 4
    @JvmStatic val UNDEFINED: Int = 5

    /** Returns a MotionStatus constant by its associated string value. */
    @JvmStatic fun byString(v: String) : Int {
        when (v) {
            "recent" -> return UPCOMING
            "open" -> return OPEN
            "tallying" -> return TALLYING
            "concluded" -> return CONCLUDED
            "inactive" -> return INACTIVE
        }
        return UNDEFINED
    }
}


package com.ramwise.lavoz.models.constants

import com.ramwise.lavoz.utils.exceptions.NativeException

object DeviceMessageType {
    @JvmStatic val ALERT: Int = 0
    @JvmStatic val BANNER: Int = 1
    @JvmStatic val INLINE: Int = 2
    @JvmStatic val UNDEFINED: Int = 3

    /** Returns a DeviceMessageType constant by its associated string value. */
    @JvmStatic fun byString(v: String) : Int {
        when (v) {
            "alert" -> return ALERT
            "banner" -> return BANNER
            "inline" -> return INLINE
        }
        return UNDEFINED
    }

    /** Returns a String based on the given DeviceMessageType constant */
    @JvmStatic fun toString(v: Int) : String {
        when (v) {
            ALERT -> return "alert"
            BANNER -> return "banner"
            INLINE -> return "inline"
        }

        throw NativeException("Passed an invalid DeviceMessageType constant.")
    }
}

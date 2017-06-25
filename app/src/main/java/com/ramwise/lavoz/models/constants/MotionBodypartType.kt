package com.ramwise.lavoz.models.constants

import com.ramwise.lavoz.utils.exceptions.NativeException


object MotionBodypartType {
    @JvmStatic val HEADER: Int = 0
    @JvmStatic val RESULTS: Int = 1
    @JvmStatic val TEXT: Int = 2
    @JvmStatic val IMAGE: Int = 3
    @JvmStatic val VIDEO: Int = 4
    @JvmStatic val TWEET: Int = 5
    @JvmStatic val YOUTUBE: Int = 6
    @JvmStatic val UNDEFINED: Int = 7

    /** Returns a MotionBodypartType constant by its associated string value. */
    @JvmStatic fun byString(v: String) : Int {
        when (v) {
            // Do not implement HEADER or RESULTS here.
            "text" -> return TEXT
            "image" -> return IMAGE
            "video" -> return VIDEO
            "tweet" -> return TWEET
            "youtube" -> return YOUTUBE
        }
        return UNDEFINED
    }

    /** Returns a String based on the given MotionBodypartType constant */
    @JvmStatic fun toString(v: Int) : String {
        when (v) {
            // Do not implement HEADER or RESULTS here.
            TEXT -> return "text"
            IMAGE -> return "image"
            VIDEO -> return "video"
            TWEET -> return "tweet"
            YOUTUBE -> return "youtube"
        }

        throw NativeException("Passed an invalid MotionBodypartType constant.")
    }
}

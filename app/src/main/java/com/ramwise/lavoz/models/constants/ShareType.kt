package com.ramwise.lavoz.models.constants


object ShareType {
    @JvmStatic val FACEBOOK: Int = 0
    @JvmStatic val TWITTER: Int = 1
    @JvmStatic val LINKEDIN: Int = 2
    @JvmStatic val EMAIL: Int = 3
    @JvmStatic val UNDEFINED: Int = 4

    /** Returns a ShareType constant by its associated string value. */
    @JvmStatic fun byString(v: String) : Int {
        when (v) {
            "facebook" -> return FACEBOOK
            "twitter" -> return TWITTER
            "linkedin" -> return LINKEDIN
            "email" -> return EMAIL
        }
        return UNDEFINED
    }
}


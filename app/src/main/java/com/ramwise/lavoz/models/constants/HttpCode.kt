package com.ramwise.lavoz.models.constants


object HttpCode {
    @JvmStatic val OK: Int = 0
    @JvmStatic val BAD_REQUEST: Int = 1
    @JvmStatic val UNAUTHORIZED: Int = 2
    @JvmStatic val FORBIDDEN: Int = 3
    @JvmStatic val NOT_FOUND: Int = 4
    @JvmStatic val METHOD_NOT_ALLOWED: Int = 5
    @JvmStatic val REQUEST_TIMEOUT: Int = 6
    @JvmStatic val CONFLICT: Int = 7
    @JvmStatic val ENTITY_TOO_LARGE: Int = 8
    @JvmStatic val EXPECTATION_FAILED: Int = 9
    @JvmStatic val UNPROCESSABLE_ENTITY: Int = 10
    @JvmStatic val INTERNAL_SERVER_ERROR: Int = 11
    @JvmStatic val BAD_GATEWAY: Int = 12
    @JvmStatic val UNDEFINED: Int = 13

    /** Returns a HttpCode constant by its associated http code value. */
    @JvmStatic fun byCode(v: Int?) : Int {
        when (v) {
            200 -> return OK
            400 -> return BAD_REQUEST
            401 -> return UNAUTHORIZED
            403 -> return FORBIDDEN
            404 -> return NOT_FOUND
            405 -> return METHOD_NOT_ALLOWED
            408 -> return REQUEST_TIMEOUT
            409 -> return CONFLICT
            413 -> return ENTITY_TOO_LARGE
            417 -> return EXPECTATION_FAILED
            422 -> return UNPROCESSABLE_ENTITY
            500 -> return INTERNAL_SERVER_ERROR
            502 -> return BAD_GATEWAY
            0 -> return UNDEFINED
        }
        return UNDEFINED
    }
}
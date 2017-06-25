package com.ramwise.lavoz.utils.factories

import com.ramwise.lavoz.network.LavozError
import com.ramwise.lavoz.network.LavozResponse

class LavozResponseFactory {

    fun <T: Any> createError(e: Throwable): LavozResponse<T, LavozError> {
        val response = LavozResponse<T, LavozError>()
        response.err = LavozError(e)

        return response
    }
}
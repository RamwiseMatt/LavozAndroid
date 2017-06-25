package com.ramwise.lavoz.network

import android.util.Log
import com.ramwise.lavoz.models.constants.HttpCode
import retrofit2.adapter.rxjava.HttpException

class LavozError(val e: Throwable? = null) {

    private var _httpCode: Int? = if (e is HttpException) e.code() else null

    /* Should contain HttpCode constants */
    val httpCode: Int?
        get() {
            return HttpCode.byCode(_httpCode)
        }

    init {
        Log.e("LavozError", e.toString())
    }
}
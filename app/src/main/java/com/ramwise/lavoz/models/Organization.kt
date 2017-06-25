package com.ramwise.lavoz.models

import com.ramwise.lavoz.models.constants.UserDesignation

import android.graphics.Color
import android.os.Parcelable

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Organization {
    @Expose
    var id: Int = 0

    @Expose
    var name: String? = null

    @Expose
    var icon: File? = null

    @Expose
    var score: Score? = null

    @SerializedName("color")
    @Expose
    var rawColor: String? = null

    val color: Int?
        get() {
            return if (rawColor != null) Color.parseColor(rawColor!!.toUpperCase()) else null
        }

    @SerializedName("designation")
    @Expose
    var rawDesignation: String? = null

    /** A [UserDesignation] constant */
    var designation: Int
        get() {
            return if (rawDesignation != null) UserDesignation.byString(rawDesignation!!)
                    else UserDesignation.UNDEFINED
        }
        set(obj) {
            rawDesignation = UserDesignation.toString(obj)
        }

}

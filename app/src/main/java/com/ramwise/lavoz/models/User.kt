package com.ramwise.lavoz.models

import android.graphics.Color
import com.ramwise.lavoz.models.constants.UserDesignation

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class User {
    companion object {
        val namespaceObject: String = "user"
        val namespaceArray: String = "users"
    }

    @Expose
    var id: Int = 0

    @Expose
    var name: String? = null

    @Expose
    var telephone: String? = null

    @Expose
    var email: String? = null

    @Expose
    var language: String? = null

    @Expose
    var organization: String? = null

    @Expose
    var anonymous: Boolean = false

    @Expose
    var incomplete: Boolean = false

    @SerializedName("is_moderator")
    @Expose
    var isModerator: Boolean = false

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

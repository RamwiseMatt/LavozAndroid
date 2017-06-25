package com.ramwise.lavoz.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.ramwise.lavoz.LavozStatic
import org.joda.time.DateTime
import org.joda.time.Duration

class AuthToken {
    @SerializedName("token")
    @Expose
    var textual: String = ""

    @SerializedName("expires_at")
    @Expose
    var rawExpiresAt: String? = null

    var expiresAt: DateTime?
        get() {
            return if (rawExpiresAt != null) DateTime.parse(rawExpiresAt,
                            LavozStatic.dateTimeFormat)
                    else null
        }
        set(value) {
            rawExpiresAt = LavozStatic.dateTimeFormat.print(value)
        }

    @Expose
    var user: User? = null

    /**
     * @return A boolean indicating whether the authentication token will expire within the next 24 hours.
     */
    fun isMaintained(): Boolean {
        return isValidWithin(Duration(1000 * 3600 * 24))
    }

    /**
     * @return A boolean indicating whether the authentication token is still valid.
     */
    fun isValid(): Boolean {
        return isValidWithin(Duration(1000 * 300))
    }

    /**
     * @param duration The time for which the token should still be valid (e.g. 12.hours)
     *
     * @return A boolean indicating whether the authentication token is still valid within a given amount of time.
     */
    private fun isValidWithin(duration: Duration): Boolean {
        val expiresAt = expiresAt
        if (expiresAt != null) {
            return (expiresAt - duration) >= DateTime()
        }
        return false
    }
}

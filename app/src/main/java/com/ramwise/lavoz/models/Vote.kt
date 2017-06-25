package com.ramwise.lavoz.models

import com.ramwise.lavoz.models.constants.VoteOption

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Vote {
    @Expose
    var motionId: Int = 0

    @SerializedName("ballot")
    @Expose
    var rawBallot: String? = null

    /** A [VoteOption] constant */
    var ballot: Int
        get() {
            return if (rawBallot != null) VoteOption.byString(rawBallot!!)
                    else VoteOption.UNDEFINED
        }
        set(obj) {
            rawBallot = VoteOption.toString(obj)
        }
}

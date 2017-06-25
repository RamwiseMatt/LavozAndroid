package com.ramwise.lavoz.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.ArrayList

class Conclusion {
    @Expose
    var motionId: Int = 0

    @SerializedName("perc_against")
    @Expose
    var percentageAgainst = 50.0

    @SerializedName("perc_for")
    @Expose
    var percentageFor = 50.0

    @SerializedName("votes_against")
    @Expose
    var votesAgainst: Int = 0

    @SerializedName("votes_for")
    @Expose
    var votesFor: Int = 0

    @Expose
    var summary: String? = null

    @Expose
    var bodyparts: ArrayList<MotionBodypart>? = null

    @Expose
    var files: ArrayList<File>? = null
}

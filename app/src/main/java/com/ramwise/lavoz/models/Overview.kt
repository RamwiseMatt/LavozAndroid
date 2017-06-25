package com.ramwise.lavoz.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.ArrayList

class Overview {
    @Expose
    lateinit var user: User

    @SerializedName("count_majority")
    @Expose
    var countMajority: Int = 0

    @SerializedName("count_minority")
    @Expose
    var countMinority: Int = 0

    @SerializedName("count_motivated")
    @Expose
    var countMotivated: Int = 0

    @SerializedName("count_received_votes")
    @Expose
    var countReceivedVotes: Int = 0

    @Expose
    var messages: ArrayList<DeviceMessage>? = null

    @SerializedName("political_scores")
    @Expose
    var politicalScores: ArrayList<Organization>? = null

}

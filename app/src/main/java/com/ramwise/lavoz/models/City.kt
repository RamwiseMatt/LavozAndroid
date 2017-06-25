package com.ramwise.lavoz.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class City {
    @Expose
    var id: Int = 0

    @Expose
    var name: String? = null

    @Expose
    var region: String? = null

    @SerializedName("country_code")
    @Expose
    var countryCode: String? = null
}

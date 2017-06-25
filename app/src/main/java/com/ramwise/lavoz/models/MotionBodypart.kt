package com.ramwise.lavoz.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.ramwise.lavoz.models.constants.MotionBodypartType

class MotionBodypart {
    @Expose
    var id: Int = 0

    @Expose
    var text: String? = null

    @Expose
    var file: File? = null

    @Expose
    var tweet: Tweet? = null

    @SerializedName("youtube_key")
    @Expose
    var youtubeKey: String? = null

    @SerializedName("allow_raw_html")
    @Expose
    var allowRawHTML: Boolean = false

    @SerializedName("type")
    @Expose
    var rawType: String? = null

    /** A [MotionBodypartType] constant */
    var type: Int
        get() {
            return if (rawType != null) MotionBodypartType.byString(rawType!!)
                    else MotionBodypartType.UNDEFINED
        }
        set(obj) {
            rawType = MotionBodypartType.toString(obj)
        }
}

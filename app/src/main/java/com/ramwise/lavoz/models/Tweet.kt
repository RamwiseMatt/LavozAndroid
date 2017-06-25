package com.ramwise.lavoz.models

import android.util.Log
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.ramwise.lavoz.LavozStatic
import org.joda.time.DateTime
import java.util.*

class Tweet {
    @Expose
    var id: Long = 0

    @Expose
    var username: String = ""

    @Expose
    var name: String = ""

    @Expose
    var text: String = ""

    @SerializedName("profile_image_url")
    @Expose
    var profileImageURLString: String? = null

    @SerializedName("at")
    @Expose
    var rawWrittenAt: String? = null

    var writtenAt: DateTime?
        get() {
            return if (rawWrittenAt != null) DateTime.parse(rawWrittenAt,
                    LavozStatic.dateTimeFormat)
            else null
        }
        set(value) {
            rawWrittenAt = LavozStatic.dateTimeFormat.print(value)
        }
}

package com.ramwise.lavoz.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.ramwise.lavoz.models.constants.DeviceMessageType
import com.ramwise.lavoz.models.constants.VoteOption

class DeviceMessage {
    @Expose
    var id: Int = 0

    @Expose
    var title: String? = null

    @Expose
    var text: String = ""

    @SerializedName("external_url")
    @Expose
    var externalURLString: String? = null

    @SerializedName("cancel_button_text")
    @Expose
    var cancelButtonText: String? = null

    @SerializedName("ok_button_text")
    @Expose
    var okButtonText: String? = null

    @Expose
    var file: File? = null

    @SerializedName("type")
    @Expose
    var rawType: String? = null

    /** A [DeviceMessageType] constant */
    var type: Int
        get() {
            return if (rawType != null) DeviceMessageType.byString(rawType!!)
            else DeviceMessageType.UNDEFINED
        }
        set(obj) {
            rawType = DeviceMessageType.toString(obj)
        }
}

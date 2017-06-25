package com.ramwise.lavoz.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Score {
    companion object {
        val namespaceObject: String = "score"
        val namespaceArray: String = "scores"
    }

    @Expose
    var id: Int = 0

    @Expose
    var points: Int = 0
}

package com.ramwise.lavoz.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.ramwise.lavoz.LavozStatic
import com.ramwise.lavoz.models.constants.ImageSizeClass
import org.joda.time.DateTime
import java.util.*

class File {
    companion object {
        val namespaceObject: String = "file"
        val namespaceArray: String = "files"
    }

    @Expose
    var id: Int = 0

    @SerializedName("aspect_ratio")
    @Expose
    var aspectRatio = 1.0

    @Expose
    var acknowledgement: String? = null

    @SerializedName("description")
    @Expose
    var descriptionText: String? = null

    @SerializedName("uploadedAt")
    @Expose
    var rawUploadedAt: String? = null

    var uploadedAt: DateTime?
        get() {
            return if (rawUploadedAt != null) DateTime.parse(rawUploadedAt, LavozStatic.dateTimeFormat)
            else null
        }
        set(value) {
            rawUploadedAt = LavozStatic.dateTimeFormat.print(value)
        }


    @Expose
    var urls: ArrayList<String>? = null


    /** Get the url string that best matches a desired image size.
     *
     * Important: currently, this method isn't fully implemented and always returns the largest
     * image url.
     *
     * @param sc A ImageSizeClass constant that indicates the desired size.
     *
     * @return The string of an image URL, or null.
     */
    fun urlByImageSizeClass(sc: Int) : String? {
        // TO-DO: Proper selection of the image. For now, just return the largest.
        val size = urls?.size ?: 0
        return if (size > 0) urls!![size - 1] else return null
    }
}

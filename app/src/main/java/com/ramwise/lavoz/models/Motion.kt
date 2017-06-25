package com.ramwise.lavoz.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.ramwise.lavoz.LavozStatic
import com.ramwise.lavoz.models.constants.MotionStatus
import com.ramwise.lavoz.models.constants.VoteOption
import com.ramwise.lavoz.utils.android.merge
import org.joda.time.DateTime
import java.util.*

class Motion {
    @Expose
    var id: Int = 0

    @Expose
    var author: User? = null

    @Expose
    var channel: Channel? = null

    @Expose
    var issue: String? = null

    @SerializedName("start")
    @Expose
    var rawStart: String? = null

    var start: DateTime?
        get() {
            return if (rawStart != null) DateTime.parse(rawStart, LavozStatic.dateTimeFormat)
            else null
        }
        set(value) {
            rawStart = LavozStatic.dateTimeFormat.print(value)
        }

    @SerializedName("end")
    @Expose
    var rawEnd: String? = null

    var end: DateTime?
        get() {
            return if (rawEnd != null) DateTime.parse(rawEnd, LavozStatic.dateTimeFormat)
            else null
        }
        set(value) {
            rawEnd = LavozStatic.dateTimeFormat.print(value)
        }

    @SerializedName("label_for")
    @Expose
    var labelFor: String? = null

    @SerializedName("label_against")
    @Expose
    var labelAgainst: String? = null

    @Expose
    var vote: Vote? = null

    @Expose
    var comment: Comment? = null

    @Expose
    var conclusion: Conclusion? = null

    @SerializedName("comments_for")
    @Expose
    var commentsFor: ArrayList<Comment>? = null

    @SerializedName("comments_against")
    @Expose
    var commentsAgainst: ArrayList<Comment>? = null

    /** A zipped ArrayList of [commentsFor] and [commentsAgainst].
     *
     * Note: these ArrayLists are re-zipped upon each call to this getter, so capture the resulting
     * ArrayList in a variable whenever possible to avoid performance degradation.
     */
    val commentsMerged: ArrayList<Comment>?
        get() {
            val commentsFor = updateRootVoteOption(commentsFor, VoteOption.AGREE)
            val commentsAgainst = updateRootVoteOption(commentsAgainst, VoteOption.DISAGREE)
            if (commentsFor != null)
                return commentsFor.merge(commentsAgainst)
            else
                // Still call merge even though commentsFor is empty. This converts the ArrayList
                // to an ArrayList of Pairs.
                return commentsAgainst?.merge(commentsFor)
        }

    @Expose
    var bodyparts: ArrayList<MotionBodypart>? = null

    @Expose
    var files: ArrayList<File>? = null

    @SerializedName("thumbnail")
    @Expose
    var rawThumbnail: File? = null

    var thumbnail: File?
        get() {
            return rawThumbnail ?: (if (files != null && files!!.size > 0) files!![0] else null)
        }
        set(obj) {
            rawThumbnail = obj
        }

    @SerializedName("status")
    @Expose
    var rawStatus: String? = null

    /** A [MotionStatus] constant */
    val status: Int
        get() {
            if (rawStatus != null) {
                val _status = MotionStatus.byString(rawStatus!!)

                return if (_status == MotionStatus.OPEN && end!!.isBefore(DateTime())) MotionStatus.TALLYING
                        else _status
            }
            return MotionStatus.UNDEFINED
        }

    /** Updates a list of Comment objects. It sets each rootVoteOption property to the desired
     * value.
     *
     * @param list The list to update
     *
     * @param rootVoteOption A VoteOption constant.
     */
    private fun updateRootVoteOption(list: ArrayList<Comment>?, rootVoteOption: Int) : ArrayList<Comment>? {
        if (list != null) {
            for (item in list) {
                item.rootVoteOption = rootVoteOption
            }
        }
        return list
    }
}

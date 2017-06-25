package com.ramwise.lavoz.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.ramwise.lavoz.LavozStatic
import com.ramwise.lavoz.models.constants.VoteOption
import org.joda.time.DateTime
import java.util.*

class Comment {
    @Expose
    var id: Int = 0

    @SerializedName("at")
    @Expose
    var rawAt: String? = null

    var at: DateTime?
        get() {
            return if (rawAt != null) DateTime.parse(rawAt, LavozStatic.dateTimeFormat)
            else null
        }
        set(value) {
            rawAt = LavozStatic.dateTimeFormat.print(value)
        }

    @Expose
    var body: String = ""

    @Expose
    var author: User? = null

    @SerializedName("comment_vote")
    @Expose
    var commentVote: CommentVote? = null

    @SerializedName("comment_vote_count")
    @Expose
    var commentVoteCount: Int = 0

    @Expose
    var responses: ArrayList<Comment>? = null

    /** Indicates how the owner of this comment voted, or how the root comment of this comment
     * voted. Note that this does not get set by the API, but must be set manually. It is therefore
     * only a helper variable.
     */
    var rootVoteOption: Int = VoteOption.UNDEFINED

    /** This indicates the depth of the comment in relation to its parents.
     * A value of 0 indicates a root level comment.
     */
    var responseDepth: Int = 0

    /** A helper variable. This indicates whether the comment view (cell) is currently expanded or
     *  not.
     *  */
    var isExpanded: Boolean = false
}

package com.ramwise.lavoz.models

import com.ramwise.lavoz.models.constants.CommentVoteOption

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class CommentVote {
    @Expose
    var id: Int = 0

    @SerializedName("value")
    @Expose
    var rawCommentVoteOption: Int = 0

    /** Should contain a CommentVoteOption constant */
    val commentVoteOption: Int
        get() {
            when (rawCommentVoteOption) {
                -1 -> return CommentVoteOption.DOWNVOTE
                0 -> return CommentVoteOption.CLEAR
                1 -> return CommentVoteOption.UPVOTE
                else -> {
                    return CommentVoteOption.UNDEFINED
                }
            }
        }
}

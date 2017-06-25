package com.ramwise.lavoz.models.constants

import com.ramwise.lavoz.utils.exceptions.NativeException

object CommentVoteOption {
    @JvmStatic val UPVOTE: Int = 0
    @JvmStatic val DOWNVOTE: Int = 1
    @JvmStatic val CLEAR: Int = 2
    @JvmStatic val UNDEFINED: Int = 3

    /** Returns a CommentVoteOption constant by its associated string value. */
    @JvmStatic fun byString(v: String) : Int {
        when (v) {
            "upvote" -> return UPVOTE
            "downvote" -> return DOWNVOTE
            "clear" -> return CLEAR
        }
        return UNDEFINED
    }

    /** Returns a String based on the given CommentVoteOption constant */
    @JvmStatic fun toString(v: Int) : String {
        when (v) {
            UPVOTE -> return "upvote"
            DOWNVOTE -> return "downvote"
            CLEAR -> return "clear"
        }

        throw NativeException("Passed an invalid CommentVoteOption constant.")
    }
}


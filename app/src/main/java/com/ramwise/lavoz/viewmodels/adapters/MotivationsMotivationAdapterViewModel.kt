package com.ramwise.lavoz.viewmodels.adapters

import android.support.v4.content.ContextCompat
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.Comment
import com.ramwise.lavoz.models.Motion
import com.ramwise.lavoz.models.constants.*
import com.ramwise.lavoz.utils.jodatime.differenceToString
import com.ramwise.lavoz.viewmodels.BaseAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotionListGenericAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotivationsGenericAdapterViewModel
import org.joda.time.DateTime
import org.joda.time.Seconds
import rx.Observable

/**
 * @param motion The Motion object to which this motivation belongs
 *
 * @param motivation The motivation ([Comment]) for which this adapter is
 *
 * @param authenticatedUserId The userId stored in the AuthService's token.
 *
 * @param forceHeartIsFilled A CommentVoteOption constant. When this is set to either .UPVOTE or
 *                          .CLEAR, the heartIsFilled property of the resulting object will be
 *                          forced to match this preference. This helps against stale data.
 *                          Pass null when there is no value to be forced.
 */
class MotivationsMotivationAdapterViewModel(motion: Motion, val motivation: Comment,
                                            authenticatedUserId: Int, forceHeartIsFilled: Int?) :
        BaseAdapterViewModel(), MotivationsGenericAdapterViewModel {

    override val type = MotivationsViewType.MOTIVATION
    override val id = motivation.id

    val authorText: String
    val authorTextColor: Int
    val motivationText: String
    val motivationTextColor: Int
    val countResponsesText = (motivation.responses?.size ?: 0).toString()
    var verifiedIconHidden: Boolean = true
    val insetByPoints = Math.min(150, motivation.responseDepth * 25)
    val actionButtonHidden: Boolean
    val isExpanded = motivation.isExpanded
    val isExpandable = (motivation.responses?.size ?: 0) > 0
    val showDeleteButton = (motivation.author?.id === authenticatedUserId)

    // These two variables might get changed from the adapter temporarily to reflect up-to-date
    // information.
    var heartIsFilled: Boolean
    var upvoteCount = motivation.commentVoteCount

    val upvoteCountText : String
        get() {
            return upvoteCount.toString()
        }


    init {
        if (motivation.body.isEmpty()) {
            authorTextColor = ContextCompat.getColor(LavozApplication.context,
                    R.color.colorGreyMediumTwo)
            motivationText = resources.getString(R.string.message_motivation_been_deleted)
            motivationTextColor = ContextCompat.getColor(LavozApplication.context,
                    R.color.colorGreyMediumTwo)
            actionButtonHidden = true
            authorText = ""
        } else {
            authorTextColor = ContextCompat.getColor(LavozApplication.context,
                    R.color.colorGreyMediumLight)
            motivationText = motivation.body
            motivationTextColor = ContextCompat.getColor(LavozApplication.context,
                    R.color.colorPrimaryLight)
            actionButtonHidden = motion.status != MotionStatus.OPEN

            val votedText: String?
            when(motivation.rootVoteOption) {
                VoteOption.AGREE -> votedText = motion.labelFor
                VoteOption.DISAGREE -> votedText = motion.labelAgainst
                else -> votedText = null
            }

            var authorText_ = (motivation.author?.name ?: resources.getString(R.string.cap_anonymous))
            if (motivation.responseDepth == 0 && votedText != null)
                authorText_ += " (" + resources.getString(R.string.nocap_voted) + " " +
                        votedText.toLowerCase() + ")"
            authorText = authorText_

            if (motivation.author?.designation != null &&
                    motivation.author?.designation != UserDesignation.INDIVIDUAL) {
                verifiedIconHidden = false
            }
        }

        when (forceHeartIsFilled) {
            CommentVoteOption.UPVOTE -> heartIsFilled = true
            CommentVoteOption.CLEAR -> heartIsFilled = false
            else -> heartIsFilled = motivation.commentVote != null
        }
    }
}

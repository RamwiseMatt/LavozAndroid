package com.ramwise.lavoz.viewmodels.adapters

import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.LavozStatic
import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.Motion
import com.ramwise.lavoz.models.constants.ImageSizeClass
import com.ramwise.lavoz.models.constants.MotionBodypartType
import com.ramwise.lavoz.models.constants.MotionStatus
import com.ramwise.lavoz.models.constants.VoteOption
import com.ramwise.lavoz.utils.jodatime.differenceToString
import com.ramwise.lavoz.viewmodels.BaseAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotionListGenericAdapterViewModel
import org.joda.time.DateTime
import org.joda.time.Seconds
import org.joda.time.format.DateTimeFormat
import rx.Observable


/** Represents an adapter for a motion that has been concluded.
 *
 * @param motion The motion to display
 */
class MotionListConcludedAdapterViewModel(motion: Motion) :
        BaseAdapterViewModel(), MotionListGenericAdapterViewModel {

    override val type = MotionStatus.CONCLUDED
    override val id = motion.id

    val mainImageURL = motion.thumbnail?.urlByImageSizeClass(ImageSizeClass.LARGE)
    val issueText = motion.issue
    val dateText: String

    var resultsDisagreeText = ""
    var resultsAgreeText = ""

    var voteIndicatorText = ""

    private val dateFormat = DateTimeFormat.forPattern(
            resources.getString(R.string.readable_date_format_major))
    init {
        dateText = dateFormat.print(motion.start).toUpperCase()

        val conclusion = motion.conclusion
        val vote = motion.vote

        if (conclusion != null) {
            resultsDisagreeText = String.format("%.0f%% %s %s", conclusion.percentageAgainst,
                    resources.getString(R.string.nocap_voted), motion.labelAgainst?.toLowerCase())
            resultsAgreeText = String.format("%.0f%% %s %s", conclusion.percentageFor,
                    resources.getString(R.string.nocap_voted), motion.labelFor?.toLowerCase())
        }

        if (vote != null) {
            when(vote.ballot) {
                VoteOption.DISAGREE -> voteIndicatorText = String.format("%s %s",
                        resources.getString(R.string.cap_you_voted), motion.labelAgainst?.toLowerCase())
                VoteOption.AGREE -> voteIndicatorText = String.format("%s %s",
                        resources.getString(R.string.cap_you_voted), motion.labelFor?.toLowerCase())
                else -> voteIndicatorText = resources.getString(R.string.cap_did_not_vote)
            }
        } else {
            voteIndicatorText = resources.getString(R.string.cap_did_not_vote)
        }


    }
}

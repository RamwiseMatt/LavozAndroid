package com.ramwise.lavoz.viewmodels.adapters

import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.Motion
import com.ramwise.lavoz.models.constants.ImageSizeClass
import com.ramwise.lavoz.models.constants.MotionBodypartType
import com.ramwise.lavoz.models.constants.MotionStatus
import com.ramwise.lavoz.utils.jodatime.differenceToString
import com.ramwise.lavoz.viewmodels.BaseAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotionListGenericAdapterViewModel
import org.joda.time.DateTime
import org.joda.time.Seconds
import rx.Observable


/** Represents an adapter for a motion that has not yet started or is currently active.
 *
 * @param motion The motion to display
 *
 * @param interval An Observable that emits a signal at a regular interval (e.g. 1 minute) which
 *                 can be used to update the line view and remaining time text.
 */
class MotionListOpenAdapterViewModel(motion: Motion, interval: Observable<Unit>) :
        BaseAdapterViewModel(), MotionListGenericAdapterViewModel {

    override val type = MotionStatus.OPEN
    override val id = motion.id

    val mainImageURL = motion.thumbnail?.urlByImageSizeClass(ImageSizeClass.LARGE)
    val issueText = motion.issue

    val now = DateTime()
    val timeAgoText: Observable<String>

    val remainingTimeFraction: Observable<Double>

    init {
        timeAgoText = interval
                .map {
                    if (now.isBefore(motion.start)) {
                        resources.getString(R.string.cap_starts_in) + " " +
                                now.differenceToString(motion.start!!)
                    } else if (now.isBefore(motion.end)) {
                        resources.getString(R.string.cap_ends_in) + " " +
                                now.differenceToString(motion.end!!)
                    } else {
                        resources.getString(R.string.cap_voting_has_closed)
                    }
                }

        remainingTimeFraction = interval
                .map {
                    val now = DateTime()
                    val total = Seconds.secondsBetween(motion.start, motion.end).seconds.toDouble()
                    val remaining = Seconds.secondsBetween(now, motion.end).seconds.toDouble()
                    val fraction = if (Math.min(total, remaining) <= 0) 0.0 else remaining / total

                    fraction
                }
    }

}

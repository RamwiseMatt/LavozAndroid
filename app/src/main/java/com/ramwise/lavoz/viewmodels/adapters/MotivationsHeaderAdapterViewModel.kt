package com.ramwise.lavoz.viewmodels.adapters

import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.Motion
import com.ramwise.lavoz.models.constants.ImageSizeClass
import com.ramwise.lavoz.models.constants.MotionBodypartType
import com.ramwise.lavoz.models.constants.MotionStatus
import com.ramwise.lavoz.models.constants.MotivationsViewType
import com.ramwise.lavoz.utils.jodatime.differenceToString
import com.ramwise.lavoz.viewmodels.BaseAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotionListGenericAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotivationsGenericAdapterViewModel
import org.joda.time.DateTime
import org.joda.time.Seconds
import rx.Observable


/** Represents an adapter for the header of a motivations page.
 *
 * @param motion The motion to display
 */
class MotivationsHeaderAdapterViewModel(motion: Motion) :
        BaseAdapterViewModel(), MotivationsGenericAdapterViewModel {

    override val type = MotivationsViewType.HEADER
    override val id = 0

    val headerImageURL = motion.thumbnail?.urlByImageSizeClass(ImageSizeClass.LARGE)
    val issueText = motion.issue
    val acknowledgementText = motion.thumbnail?.acknowledgement
}

package com.ramwise.lavoz.utils.factories

import android.util.SparseIntArray
import com.ramwise.lavoz.models.Comment
import com.ramwise.lavoz.models.Motion
import com.ramwise.lavoz.models.MotionBodypart
import com.ramwise.lavoz.models.Organization
import com.ramwise.lavoz.models.constants.MotionBodypartType
import com.ramwise.lavoz.models.constants.MotionStatus
import com.ramwise.lavoz.viewmodels.adapters.*
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotionListGenericAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotivationsGenericAdapterViewModel
import java.util.*
import rx.Observable

/** This factory converts an ArrayList of model objects into a List of matching AdapterViewModels.
 *
 * There is a build() method for each accepted type. If the provided array (via the first parameter)
 * equals null, then an empty list will be returned.
 */
class AdapterViewModelFactory {
    /** Builds an array of [RecommendationAdapterViewModel]s for use on the Recommendations Fragment
     */
    fun build(objs: ArrayList<Organization>?) : List<RecommendationAdapterViewModel> {
        return objs?.map { RecommendationAdapterViewModel(it) }
                ?: ArrayList<RecommendationAdapterViewModel>()
    }

    /** Builds an array of [MotionListGenericAdapterViewModel]s for use on the MotionList Fragment
     *
     * @param objs An ArrayList of [Motion] objects
     *
     * @param interval An Observable that emits a signal at a regular interval (e.g. 1 minute) which
     *                 can be used to update the line view and remaining time text.
     */
    fun build(objs: List<Motion>?, interval: Observable<Unit>) :
            ArrayList<MotionListGenericAdapterViewModel> {
        val constructed = objs?.map {
            val vm: MotionListGenericAdapterViewModel
            when (it.status) {
                MotionStatus.TALLYING,
                MotionStatus.CONCLUDED -> vm = MotionListConcludedAdapterViewModel(it)
                else -> vm = MotionListOpenAdapterViewModel(it, interval)
            }
            vm
        }

        val results = ArrayList<MotionListGenericAdapterViewModel>()
        if (constructed != null)
            results.addAll(constructed)

        return results
    }

    /** Builds an array of [MotivationsGenericAdapterViewModel]s for use on the MotionList Fragment
     *
     * @param motion The [Motion] to which these motivations belong
     *
     * @param objs A List of [Motivation] objects
     *
     * @param authenticatedUserId The userId of the authenticated user. This is required by the
     *                            AdapterViewModels to determine whether a trash button should be
     *                            visible or not, and so this value should always be correct.
     *
     * @param cachedHeartData A SparseIntArray that contains information about what comments the
     *                        user has upvoted/cleared since the last API data refresh.
     *                        The keys are comment id's, the values CommentVoteOption constants.
     */
    fun build(motion: Motion, objs: List<Comment>?, authenticatedUserId: Int,
              cachedHeartData: SparseIntArray) :
            ArrayList<MotivationsGenericAdapterViewModel> {
        val header = MotivationsHeaderAdapterViewModel(motion)
        val constructed = objs?.map {
            val cached_ = cachedHeartData.get(it.id, -1)
            val forceHeartIsFilled = if (cached_ == -1) null else cached_

            MotivationsMotivationAdapterViewModel(motion, it, authenticatedUserId,
                    forceHeartIsFilled) }

        val results = ArrayList<MotivationsGenericAdapterViewModel>(arrayListOf(header))
        if (constructed != null)
            results.addAll(constructed)

        return results
    }
}
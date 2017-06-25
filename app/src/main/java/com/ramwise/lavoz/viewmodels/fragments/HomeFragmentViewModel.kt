package com.ramwise.lavoz.viewmodels.fragments

import android.support.v4.content.ContextCompat
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.utils.factories.LavozResponseFactory
import com.ramwise.lavoz.utils.factories.ObservableFactory
import com.ramwise.lavoz.models.Motion
import com.ramwise.lavoz.models.constants.MotionStatus

import com.ramwise.lavoz.models.constants.VoteOption
import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.network.LavozService

import javax.inject.Inject
import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.Vote
import com.ramwise.lavoz.models.constants.HttpCode
import com.ramwise.lavoz.network.LavozError
import com.ramwise.lavoz.network.LavozResponse
import com.ramwise.lavoz.utils.sharing.ShareMotion
import com.ramwise.lavoz.utils.*
import com.ramwise.lavoz.utils.jodatime.differenceToString
import com.ramwise.lavoz.utils.rx.*
import com.ramwise.lavoz.viewmodels.BaseViewModel

import org.joda.time.DateTime
import org.joda.time.Seconds
import rx.Observable
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * @param forMotionId Indicates whether this is a viewModel for a specific motion. If null, it is
 *                    assumed to be for the latest motion (i.e. /now)
 *
 * @param asAdviceAid Indicates whether this belongs to a fragment that is used as an advice aid.
 *
 * @param clickReadMore A click on the read more button
 *
 * @param clickMotivations A click on the motivations button
 *
 * @param clickShare A click on the clickshare button
 *
 * @param initiateVote Should be a VoteOption constant
 *
 * @param initiateVoteConfirmation Should be a VoteOption constant
 */
class HomeFragmentViewModel(
        forMotionId: Int?,
        asAdviceAid: Boolean,
        clickReadMore: Observable<Unit>,
        clickMotivations: Observable<Unit>,
        clickShare: Observable<Unit>,
        initiateVote: Observable<Int>,
        initiateVoteConfirmation: Observable<Int>): BaseViewModel() {

    @Inject lateinit var lavozService: LavozService
    @Inject lateinit var authService: AuthenticationService

    /** Should contain MotionStatus constants */
    val motionStatus: Observable<Int>
    val motionLabelFor: Observable<String>
    val motionLabelAgainst: Observable<String>
    val motionId: Observable<Int>
    val motionIssue: Observable<String>

    /* Should contain VoteOption constants, or null */
    val motionBallot: Observable<Int?>

    val motionRemainingTimeText: Observable<String>
    val motionRemainingTimeLabelColor: Observable<Int>
    val motionRemainingTimeFraction: Observable<Double>

    val resultsAgreeText: Observable<String>
    val resultsDisagreeText: Observable<String>
    val resultsAgreeFraction: Observable<Double>
    val resultsDisagreeFraction: Observable<Double>
    val resultsAgreeVotecountText: Observable<String>
    val resultsDisagreeVotecountText: Observable<String>

    /** Shows the text of either "Read more" or "View results" (or the like), depending on the
     * motion status.
     */
    val moreDetailsText: Observable<String>

    /** Indicates whether the current motion has a more detailed version available (more than just
     * the issue text).
     */
    val fullMotionAvailable: Observable<Boolean>

    /** Indicates whether the current motion has a more detailed conclusion available (more than
     * just percentage results).
     */
    val fullConclusionAvailable: Observable<Boolean>

    /** Indicates whether the read more button should be hidden or not (usually hiddeh when no
     * motion has yet loaded).
     */
    val readMoreButtonHidden: Observable<Boolean>

    /** Indicates whether the read more button should be hidden or not (usually hiddeh when no
     * motion has yet loaded).
     */
    val motivationsuttonHidden: Observable<Boolean>

    /** Indicates whether the read more button should be hidden or not (usually hiddeh when no
     * motion has yet loaded).
     */
    val shareButtonHidden: Observable<Boolean>

    /** Indicates whether the vote button container should be visible (during motion = open state,
     * usually).
     * */
    val voteButtonContainerHidden: Observable<Boolean>

    /** Indicates whether the result button container should be visible
     * during motion = tallying/conclusion state, usually).
     */
    val resultsContainerHidden: Observable<Boolean>

    /** A helper RxVariable to indicate whether the user has already voted before. Should contain
     * VoteOption constants.
     */
    private val hasAlreadyVoted = RxVariable(VoteOption.UNDEFINED)

    /** Emits whenever the API has successfully cast the user's vote, and the vote that comes with
     * the actual motion object.
     *
     * Should contain VoteOption constants.
     */
    val vote: Observable<Int>

    /** Emits a signal whenever the user taps on a vote button when the motion is closed
    The emitted string is the message to be displayed to the user.
     */
    val errorMotionClosed: Observable<String>

    /** Emits all errors that were caused by the API when trying to vote. The associated values
     * are the title and the error message.
     */
    val errorVoteCast: Observable<Pair<String, String>>

    /** Emits a signal whenever the user has already voted on this motion and wishes to vote again.
     * The emitted value is a the voteOption that the user intended to use in the vote.
     *
     * Should contain VoteOption constants.
     */
    val voteRequiresChangeConfirmationFirst: Observable<Int>

    /** Emits a signal whenever the user has already voted on this motion and wishes to vote on the
     * same ballot again. The emitted value is a string to be displayed to the user.
     */
    val voteAlreadyCastMessage: Observable<String>

    /** Indicates whether a vote is currently being cast.
     * A successful cast is debounced by 1.5 seconds (for the effect of doing something of
     * significance), but ends immediately on failure.
     */
    val voteCastActivityIndicator: Observable<Boolean>

    /** Emits a signal whenever the read more button is tapped.
     * The value emitted is the motion id the target VC should use.
     */
    val navigateToMoreDetails: Observable<Int>

    /** Emits a signal whenever the user should navigate to the motivations page.
     * The value emitted is the motion id the target VC should use.
     */
    val navigateToMotivations: Observable<Int>

    /** Emits a signal whenever the user should navigate to the advice aid page when the process is
     * completed.
     * */
    val navigateToAdviceAidCompleted: Observable<Unit>

    /** Emits a signal whenever the user wishes to share a motion.
     * The value emitted is a [ShareMotion] object.
     */
    val showShareOptions: Observable<ShareMotion>

    /** Emits a signal when all tokens should be removed via the AuthenticationService. */
    val removeAuthToken: Observable<Unit>

    /** Emits a signal whenever the user should navigate to the motivations page.
     *
     * Emits a void when the user encountered either an unauthenticated error or an unsupported
     * client error.
     */
    val navigateToLoginActivity: Observable<Unit>

    /** Emits a value whenever a ballot was successfully cast in advice aid mode. */
    val redirectToAdviceAidNext: Observable<Unit>

    /** Emits a boolean that indicates whether the Activity/Fragment should show/hide its loading
     * spinner in the overlay in x seconds.
     * Any false signal should cancel a timer controller by the Activity/Fragment, any true should
     * activate such a timer.
     */
    val prepareLayoutOverlayHUDLoader: Observable<Boolean>

    val timerContainerHidden = Observable.just(asAdviceAid)

    val adviceAidLogoHidden = Observable.just(!asAdviceAid)


    private val responseFactory = LavozResponseFactory()

    init {
        LavozApplication.networkComponent.inject(this)

        val activityIndicator = RxActivityIndicator()
        val activityIndicatorVoting = RxActivityIndicator()

        prepareLayoutOverlayHUDLoader = activityIndicator.asObservable()

        val motionFromAPI = ObservableFactory()
                .engagedInterval(engaged)
                .flatMap {
                    val o: Observable<LavozResponse<Motion, LavozError>>
                    if (asAdviceAid) {


                        o = lavozService.getMotionForAdviceAid()
                    } else {
                        o = if(forMotionId != null) lavozService.getMotion(forMotionId)
                            else                    lavozService.getMotionNow()
                    }
                    o.subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicator)
                            .onErrorReturn { responseFactory.createError<Motion>(it) }
                }
                .shareReplay(1)
                .retain(retainBag)

        val motion = motionFromAPI
                .map { it.motion }
                .unwrap()

        motionStatus = motion.map { it.status }
        motionLabelFor = motion.map { it.labelFor }
        motionLabelAgainst = motion.map { it.labelAgainst }
        motionId = motion.map { it.id }
        motionIssue = motion.map { it.issue }
        motionBallot = motion.map {
                    val ballot = it.vote?.ballot
                    if (ballot != null) {
                        // Introduce a small side-effect. This is necessary to avoid circular
                        // referencing.
                        hasAlreadyVoted.value = ballot
                    }
                    ballot
                }

        fullMotionAvailable = motion.map { it.bodyparts != null && it.bodyparts!!.size > 0 }
        fullConclusionAvailable = motion.map {
            (it.conclusion?.bodyparts != null && it.conclusion!!.bodyparts!!.size > 0)
        }

        moreDetailsText = motionStatus.map {
            if ((it == MotionStatus.TALLYING || it == MotionStatus.CONCLUDED) && !asAdviceAid)
                resources.getString(R.string.button_see_results)
            else resources.getString(R.string.button_read_more)
        }


        readMoreButtonHidden = motion.map { false }.startWith(true)
        motivationsuttonHidden = motion.map { false }.startWith(true)
        shareButtonHidden = motion.map { false }.startWith(true)

        // MARK: - Organize timer-related data

        val updateUITimerInterval = Observable.interval(5, TimeUnit.SECONDS).startWith(0).shareReplay(1)
        val start = motion.map { it.start }.unwrap().shareReplay(1)
        val end = motion.map { it.end }.unwrap().shareReplay(1)

        resultsContainerHidden = Observable
                .combineLatest(updateUITimerInterval, motion) { t1, t2 ->
                    t2.status == MotionStatus.OPEN || asAdviceAid == true
                }
                .startWith(true)

        voteButtonContainerHidden = Observable
                .combineLatest(updateUITimerInterval, motion) { t1, t2 ->
                    t2.status != MotionStatus.OPEN && asAdviceAid == false
                }
                .startWith(true)

        motionRemainingTimeText = Observable.combineLatest(updateUITimerInterval, start, end,
                { t1, t2, t3 ->
                    val now = DateTime()
                    val str: String
                    if (now.isBefore(t2)) {
                        str = resources.getString(R.string.cap_starts_in) + " " +
                                now.differenceToString(t2)
                    } else if (now.isBefore(t3)) {
                        str = resources.getString(R.string.cap_ends_in) + " " +
                                now.differenceToString(t3)
                    } else {
                        str = resources.getString(R.string.cap_voting_has_closed)
                    }
                    str
                })

        motionRemainingTimeLabelColor = Observable.combineLatest(updateUITimerInterval, start, end,
                { t1, t2, t3 ->
                    val now = DateTime()
                    val color: Int

                    if (now.isAfter(t2) && (Seconds.secondsBetween(now, t3).seconds in 1..300)) {
                        color = ContextCompat.getColor(LavozApplication.context,
                                R.color.colorAlert)
                    } else {
                        color = ContextCompat.getColor(LavozApplication.context,
                                R.color.colorGreyMedium)
                    }
                    color
                })

        motionRemainingTimeFraction = Observable.combineLatest(updateUITimerInterval, start, end,
                { t1, t2, t3 ->
                    val now = DateTime()
                    val total = Seconds.secondsBetween(t2, t3).seconds.toDouble()
                    val remaining = Seconds.secondsBetween(now, t3).seconds.toDouble()
                    val fraction = if (Math.min(total, remaining) <= 0) 0.0 else remaining / total

                    fraction
                })

        // MARK: - Vote to API

        val voteButtonsTapped = initiateVote
                .withLatestFrom(motionStatus, { voteOption: Int,
                                                motionStatus: Int ->
                    if (motionStatus == MotionStatus.OPEN || asAdviceAid == true)
                        voteOption
                    else null
                })

        errorMotionClosed = voteButtonsTapped
                .filter { it == null }
                .map { LavozApplication.context.resources.getString(R.string.error_vote_ended) }

        /** The emitted value is Pair. The first value is a boolean that indicates whether the new
         * vote is the same as a previous vote. The second value is the new vote as a VoteOption.
         * It only emits values if it has been established that the user has previously voted.
         */
        val voteIsRepeatAction = voteButtonsTapped
                .unwrap()
                .withLatestFrom(hasAlreadyVoted.asObservable().distinctUntilChanged(), {
                        voteOption: Int, old: Int ->
                    Pair(old, voteOption)
                })
                .filter { it.first != VoteOption.UNDEFINED }
                .map {
                    Pair(it.first == it.second && asAdviceAid == false, it.second)
                }

        voteRequiresChangeConfirmationFirst = voteIsRepeatAction
                .filter { !(it.first) }
                .map { it.second }

        voteAlreadyCastMessage = voteIsRepeatAction
                .filter { it.first }
                .map { it.second }
                .withLatestFrom(motion, { voteOption: Int, motion: Motion ->
                    val label: String?
                    when (voteOption) {
                        VoteOption.AGREE -> label = motion.labelFor?.toLowerCase()
                        VoteOption.DISAGREE -> label = motion.labelAgainst?.toLowerCase()
                        else -> label = null
                    }

                    if (label != null)
                        String.format(resources.getString(R.string.message_already_voted_x), label)
                    else
                        resources.getString(R.string.message_already_voted)
                })

        val voteDoesNotRequireConfirmation = voteButtonsTapped
                .withLatestFrom(hasAlreadyVoted.asObservable().distinctUntilChanged(), {
                        voteOption: Int?, old: Int ->
                    if (old != VoteOption.UNDEFINED) null else voteOption
                })
                .unwrap()

        val voteCanProceed = Observable.merge(
                initiateVoteConfirmation,
                voteDoesNotRequireConfirmation
                        .share())

        val voteToAPI = voteCanProceed
                .withLatestFrom(motionId, { voteOption: Int, motionId: Int ->
                    Pair(voteOption, motionId)
                })
                .flatMap {
                    val voteOption = it.first

                    lavozService
                            .castVote(it.second, it.first)
                            .subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicatorVoting)
                            .onErrorReturn { responseFactory.createError<Vote>(it) }
                            .map {
                                if (it.err == null) {
                                    // Place the original vote object back in the output.
                                    // Do this by creating a dummy Vote object first.
                                    it.vote = Vote()
                                    it.vote?.ballot = voteOption
                                }
                                it
                            }
                }
                .shareReplay(1)
                .retain(retainBag)

        errorVoteCast = voteToAPI
                .map { it.err }
                .unwrap()
                .map {
                    when (it.httpCode) {
                        HttpCode.CONFLICT -> Pair(
                                        resources.getString(R.string.cap_oops),
                                        resources.getString(R.string.error_one_vote_per_phone)
                                )
                        else -> Pair(
                                        resources.getString(R.string.cap_error),
                                        resources.getString(R.string.error_try_voting_later)
                                )
                    }
                }

        val successfulVoteCast = voteToAPI
                .map { it.vote?.ballot }
                .unwrap()
                .map {
                    // Introduce a small side-effect. This is necessary to avoid circular
                    // referencing.
                    hasAlreadyVoted.value = it; it
                }
                .share()

        vote = Observable
                .merge(
                        successfulVoteCast,
                        motionBallot
                )
                .unwrap()
                .shareReplay(1)
                .retain(retainBag)

        val standardDelay: Long = if (asAdviceAid) 750 else 1500

        val privateVoteCastActivityIndicatorStart = activityIndicatorVoting.asObservable()
                .filter { it }
        val privateVoteCastActivityIndicatorEnd = activityIndicatorVoting.asObservable()
                .filter { !it }
        voteCastActivityIndicator = Observable.merge(
                privateVoteCastActivityIndicatorStart,
                Observable.merge(
                        errorVoteCast.map { false },
                        privateVoteCastActivityIndicatorEnd.debounce(standardDelay,
                                TimeUnit.MILLISECONDS)
                )
        )

        if (asAdviceAid)
            redirectToAdviceAidNext = successfulVoteCast.map { (Unit) }
        else
            redirectToAdviceAidNext = Observable.empty()

        navigateToMoreDetails = clickReadMore
                .withLatestFrom(motionId, { click: (Unit), motionId: Int ->
                    motionId
                })

        navigateToMotivations = clickMotivations
                .withLatestFrom(motionId, { click: (Unit), motionId: Int ->
                    motionId
                })

        navigateToAdviceAidCompleted = motionFromAPI
                .map { it.err }
                .unwrap()
                .map {
                    when (it.httpCode) {
                        HttpCode.NOT_FOUND -> true
                        else -> false
                    }
                }
                .filter { it && asAdviceAid }
                .map { (Unit) }

        showShareOptions = clickShare
                .withLatestFrom(motion, { click: (Unit), motion: Motion ->
                    ShareMotion(motion, resources)
                })

        resultsAgreeText = motion
                .filter { it.conclusion != null }
                .map { Pair(it.conclusion!!.percentageFor, it.labelFor!! ) }
                .map {
                    val resId = if (it.first == Math.floor(it.first))
                        R.string.conclusion_formatted_zero_decimal
                    else R.string.conclusion_formatted_zero_decimal
                    String.format(resources.getString(resId),
                            it.first, resources.getString(R.string.nocap_voted),
                            it.second.toUpperCase())
                }

        resultsDisagreeText = motion
                .filter { it.conclusion != null }
                .map { Pair(it.conclusion!!.percentageAgainst, it.labelAgainst!! ) }
                .map {
                    val resId = if (it.first == Math.floor(it.first))
                        R.string.conclusion_formatted_zero_decimal
                    else R.string.conclusion_formatted_zero_decimal
                    String.format(resources.getString(resId),
                            it.first, resources.getString(R.string.nocap_voted),
                            it.second.toUpperCase())
                }

        resultsAgreeFraction = motion.map { it.conclusion?.percentageFor }.unwrap()
                .map { it / 100 }
        resultsDisagreeFraction = motion.map { it.conclusion?.percentageAgainst }.unwrap()
                .map { it / 100 }

        resultsAgreeVotecountText = motion.map { it.conclusion?.votesFor }.unwrap()
                .map { "$it" }
        resultsDisagreeVotecountText = motion.map { it.conclusion?.votesAgainst }.unwrap()
                .map { "$it" }

        // MARK: - Other UI observables

        val userIsNotAuthenticated = motionFromAPI
                .map { it.err }
                .unwrap()
                .map {
                    when (it.httpCode) {
                        HttpCode.UNAUTHORIZED,
                        HttpCode.EXPECTATION_FAILED -> true
                        else -> false
                    }
                }
                .filter { it }
                .share()

        val signOutToAPI = userIsNotAuthenticated
                .flatMap {
                    lavozService.signOut()
                            .subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicator)
                            .onErrorReturn { responseFactory.createError<Unit>(it) }
                }
                .shareReplay(1)

        removeAuthToken = signOutToAPI
                .map { (Unit) }

        navigateToLoginActivity = signOutToAPI
                .map { (Unit) }
    }
}
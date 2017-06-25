package com.ramwise.lavoz.viewmodels.fragments

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.util.Log
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.utils.factories.LavozResponseFactory
import com.ramwise.lavoz.utils.factories.ObservableFactory
import com.ramwise.lavoz.models.Motion

import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.network.LavozService

import javax.inject.Inject
import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.MotionBodypart
import com.ramwise.lavoz.models.Vote
import com.ramwise.lavoz.models.constants.*
import com.ramwise.lavoz.utils.sharing.ShareMotion
import com.ramwise.lavoz.utils.*
import com.ramwise.lavoz.utils.factories.AdapterViewModelFactory
import com.ramwise.lavoz.utils.jodatime.differenceToString
import com.ramwise.lavoz.utils.rx.*
import com.ramwise.lavoz.utils.webviews.LavozHTML
import com.ramwise.lavoz.viewmodels.BaseViewModel

import org.joda.time.DateTime
import org.joda.time.Seconds
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func2
import rx.functions.Func3
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit


class MotionFragmentViewModel(
        forMotionId: Int,
        clickMotivations: Observable<Unit>): BaseViewModel() {

    @Inject lateinit var lavozService: LavozService
    @Inject lateinit var authService: AuthenticationService

    /** Emits a boolean that indicates whether the Activity/Fragment should show/hide its loading
     * spinner in the overlay in x seconds.
     * Any false signal should cancel a timer controller by the Activity/Fragment, any true should
     * activate such a timer.
     */
    val prepareLayoutOverlayHUDLoader: Observable<Boolean>

    val motionHeaderImageURL: Observable<String>
    val motionIssue: Observable<String>
    val motionTimeAgoText: Observable<String>
    val motionHeaderAcknowledgement: Observable<String>

    /** Emits a boolean that indicates whether the results container should be hidden or not */
    val resultsContainerHidden: Observable<Boolean>

    val resultsAgreeText: Observable<String>
    val resultsDisagreeText: Observable<String>
    val resultsAgreeFraction: Observable<Double>
    val resultsDisagreeFraction: Observable<Double>
    val resultsAgreeVotecountText: Observable<String>
    val resultsDisagreeVotecountText: Observable<String>

    /** Emits a signal whenever the motivations button is tapped.
     * The value emitted is the motion id the target VC should use.
     */
    val navigateToMotivations: Observable<Int>

    /** Emits a signal whenever the fragment should tell the activity to move backwards. This is
     * mostly useful when the Fragment has run into a 404 or the like.
     */
    val navigateToPreviousFragment: Observable<Unit>


    /** Emits a signal whenever the motion could not be found.
     * The emitted string is the message to be displayed to the user.
     */
    val errorMotionNotFound: Observable<String>

    /** The HTML that should be displayed by the WebView */
    val webviewHTML: Observable<String>

    private val responseFactory = LavozResponseFactory()

    private val lavozHTMLConverter = LavozHTML()

    private val tweetDateFormatter = DateTimeFormat.forPattern(
            resources.getString(R.string.readable_date_format_full))

    init {
        LavozApplication.networkComponent.inject(this)

        val activityIndicator = RxActivityIndicator()

        prepareLayoutOverlayHUDLoader = activityIndicator.asObservable()

        val motionFromAPI = ObservableFactory()
                .engagedInterval(engaged)
                .flatMap {
                    lavozService
                            .getMotion(forMotionId)
                            .subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicator)
                            .onErrorReturn { responseFactory.createError<Motion>(it) }
                }
                .shareReplay(1)
                .retain(retainBag)

        val motion = motionFromAPI
                .map { it.motion }
                .unwrap()

        val motionId = motion.map { it.id }

        motionIssue = motion.map { it.issue }

        motionHeaderImageURL = motion
                .map { it.thumbnail?.urlByImageSizeClass(ImageSizeClass.LARGE) }
                .unwrap()

        motionHeaderAcknowledgement = motion
                .map { it.thumbnail?.acknowledgement }
                .unwrap()

        motionTimeAgoText = motion.map {
            val now = DateTime()
            if (it.start == null || it.end == null) {
                ""
            } else {
                if (now.isBefore(it.start)) {
                    resources.getString(R.string.cap_starts_in) + " " +
                            now.differenceToString(it.start!!)
                } else if (now.isBefore(it.end)) {
                    resources.getString(R.string.cap_ends_in) + " " +
                            now.differenceToString(it.end!!)
                } else {
                    String.format(resources.getString(R.string.cap_ended_x_ago),
                            it.end!!.differenceToString(now))
                }
            }
        }

        resultsContainerHidden = motion.map { (it.conclusion == null) }

        resultsAgreeText = motion
                .filter { it.conclusion != null }
                .map { Pair(it.conclusion!!.percentageFor, it.labelFor!! ) }
                .map {
                    val resId = if (it.first == Math.floor(it.first))
                        R.string.conclusion_formatted_zero_decimal
                    else R.string.conclusion_formatted_one_decimal
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
                    else R.string.conclusion_formatted_one_decimal
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

        val bodypartsAsHTML = motion
                .map { it.bodyparts }
                .unwrap()
                .map { _bodypartsToHTML(it, false) }

        val conclusionBodypartsAsHTMLOrNull = motion
                .map { it.conclusion?.bodyparts }
                .map { if (it != null) _bodypartsToHTML(it, true) else null }

        webviewHTML = Observable.combineLatest(
                bodypartsAsHTML, conclusionBodypartsAsHTMLOrNull) { a, b ->
                    val rawText = if (b != null && b.isNotEmpty()) b + a else a

                    lavozHTMLConverter.htmlLavozStyle(rawText)
                }

        navigateToMotivations = clickMotivations
                .withLatestFrom(motionId, { click: (Unit), motionId: Int ->
                    motionId
                })

        val motionNotFoundError = motionFromAPI
                .map { it.err }
                .unwrap()
                .map {
                    when (it.httpCode) {
                        HttpCode.NOT_FOUND -> true
                        else -> false
                    }
                }
                .filter { it }

        errorMotionNotFound = motionNotFoundError
                .map { resources.getString(R.string.cap_not_found) }

        navigateToPreviousFragment = motionNotFoundError
                .delay(200, TimeUnit.MILLISECONDS)
                .map { (Unit) }
    }

    /** Takes an ArrayList of MotionBodypart items and turns them into a single HTML representation
     * of their content.
     */
    private fun _bodypartsToHTML(bodyparts: ArrayList<MotionBodypart>, forConclusion: Boolean = false) : String {
        var result = bodyparts
                .map { bodypart ->
                    when (bodypart.type) {
                        MotionBodypartType.TEXT -> {
                            lavozHTMLConverter.generateTextHTML(bodypart.text)
                        }
                        MotionBodypartType.IMAGE -> {
                            lavozHTMLConverter.generateImageHTML(
                                    bodypart.file?.urlByImageSizeClass(ImageSizeClass.LARGE),
                                    bodypart.file?.descriptionText,
                                    bodypart.file?.acknowledgement)
                        }
                        MotionBodypartType.TWEET -> {
                            val dateText = tweetDateFormatter.print(bodypart.tweet?.writtenAt)
                            lavozHTMLConverter.generateTweetHTML(
                                    bodypart.tweet?.text, dateText, bodypart.tweet?.name,
                                    bodypart.tweet?.username)
                        }
                        MotionBodypartType.YOUTUBE -> {
                            lavozHTMLConverter.generateYoutubeHTML(bodypart.youtubeKey,
                                    resources.displayMetrics)
                        }
                        else -> null as String?
                    }
                }
                .filter { it != null }
                .map { it!! }
                .fold("") { l, r -> l + r }

        if (forConclusion && result.isNotEmpty()) {
            // Add a visual separator.
            result += lavozHTMLConverter.generateConclusionSeperatorHTML(
                    resources.getString(R.string.cap_background))
        }

        return result
    }
}
package com.ramwise.lavoz.viewmodels.fragments

import android.support.v4.content.ContextCompat
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.LavozConstants
import com.ramwise.lavoz.utils.factories.LavozResponseFactory
import com.ramwise.lavoz.utils.factories.ObservableFactory

import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.network.LavozService

import javax.inject.Inject
import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.*
import com.ramwise.lavoz.models.constants.*
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
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * @param forMotionId The Motion id
 *
 * @param motionLabelFor The label on the relevant motion that represents voting for/agree.
 *
 * @param motionLabelAgainst The label on the relevant motion that represents voting against/disagree
 *
 * @param rootVoteOption A VoteOption constant. If the user is creating a root comment, this should
 *                       indicate how they voted. If this is a reply, this can be the value that
 *                       the root comment in the chain voted as.
 *
 * @param replyToCommentId The Comment id to which is being replied. In case this is not a reply
 * but a root comment, pass null.
 *
 * @param replytoAuthorName The name of the author to which is being replied. In case this is not a
 * reply but a root comment, pass null.
 *
 * @param inputChanged An Observable that emits thethe user-added text in the input field.
 */
class AddMotivationFragmentViewModel(forMotionId: Int,
                                     motionLabelFor: String,
                                     motionLabelAgainst: String,
                                     rootVoteOption: Int,
                                     replyToCommentId: Int?,
                                     replytoAuthorName: String?,
                                     inputChanged: Observable<CharSequence>,
                                     cancelButtonClicked: Observable<Unit>,
                                     addButtonClicked: Observable<Unit>): BaseViewModel() {

    @Inject lateinit var lavozService: LavozService
    @Inject lateinit var authService: AuthenticationService

    private val maxLength = 420

    private val charactersLeftAsInt: Observable<Int> = inputChanged
            .map { (maxLength - it.length) }
            .shareReplay(1)

    /** Emits a signal that indicates the number of characters left */
    val charactersLeft: Observable<String> = charactersLeftAsInt.map { it.toString() }

    /** Emits a signal that indicates whether the "add" button should be clickable or not. */
    val addButtonEnabled: Observable<Boolean> = charactersLeftAsInt
            .map { it >= 0 && it < maxLength }
            .startWith(false)
            .shareReplay(1)

    val tagLine: Observable<String>

    /** Emits a signal whenever the user should navigate to the add motivation page. */
    val navigateToMotivations: Observable<Unit>

    /** Emits a signal whenever the user wishes to navigate to the add motivations page but cannot.
     *
     * The emitted value is the string to be displayed to the user.
     */
    val errorCannotAddMotivation: Observable<String>

    /** Emits a boolean that indicates whether the Activity/Fragment should show/hide its loading
     * spinner in the overlay in x seconds, with an "adding comment" text.
     * Any false signal should cancel a timer controller by the Activity/Fragment, any true should
     * activate such a timer.
     */
    val addingMotivationOverlayHUDLoader: Observable<Boolean>

    private val responseFactory = LavozResponseFactory()

    init {
        LavozApplication.networkComponent.inject(this)

        val activityIndicator = RxActivityIndicator()

        addingMotivationOverlayHUDLoader = activityIndicator.asObservable()

        val _tagLine: String
        if (replyToCommentId != null) {
            if (replytoAuthorName != null) {
                _tagLine = String.format(resources.getString(R.string.cap_reply_to_motivation),
                        replytoAuthorName)
            } else {
                _tagLine = String.format(resources.getString(R.string.cap_reply_to_motivation),
                        resources.getString(R.string.cap_anonymous).toLowerCase())
            }
        } else {
            when(rootVoteOption) {
                VoteOption.AGREE -> _tagLine = String.format(
                        resources.getString(R.string.question_why_vote_x), motionLabelFor.toLowerCase())
                VoteOption.DISAGREE -> _tagLine = String.format(
                        resources.getString(R.string.question_why_vote_x), motionLabelAgainst.toLowerCase())
                else -> _tagLine = resources.getString(R.string.question_why_vote)
            }
        }

        tagLine = Observable.just(_tagLine)

        val motivationToAPI = addButtonClicked
                .withLatestFrom(addButtonEnabled) { ignore: Unit, isEnabled: Boolean -> isEnabled }
                .filter { it }
                .withLatestFrom(inputChanged.map { it.toString() }) { ignore: Boolean,
                                                                      input: String -> input }
                .flatMap {
                    lavozService
                            .commentWrite(forMotionId, it, rootVoteOption, replyToCommentId)
                            .subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicator)
                            .onErrorReturn { responseFactory.createError<Comment>(it) }
                }
                .shareReplay(1)

        val motivation = motivationToAPI
                .map { it.comment }
                .unwrap()

        errorCannotAddMotivation = motivationToAPI
                .map { it.err }
                .unwrap()
                .map {
                    when (it.httpCode) {
                        HttpCode.CONFLICT -> resources.getString(R.string.error_already_motivated)
                        else -> resources.getString(R.string.error_not_added_motivation)
                    }
                }

        navigateToMotivations = Observable
                .merge(cancelButtonClicked, motivation).map { (Unit) }
    }
}
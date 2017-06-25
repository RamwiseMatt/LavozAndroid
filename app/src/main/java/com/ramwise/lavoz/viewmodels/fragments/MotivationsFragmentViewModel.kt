package com.ramwise.lavoz.viewmodels.fragments

import android.support.v4.content.ContextCompat
import android.util.SparseIntArray
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.utils.factories.LavozResponseFactory
import com.ramwise.lavoz.utils.factories.ObservableFactory
import com.ramwise.lavoz.models.Motion

import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.network.LavozService

import javax.inject.Inject
import com.ramwise.lavoz.R
import com.ramwise.lavoz.models.Comment
import com.ramwise.lavoz.models.CommentVote
import com.ramwise.lavoz.models.Vote
import com.ramwise.lavoz.models.constants.*
import com.ramwise.lavoz.network.LavozError
import com.ramwise.lavoz.utils.sharing.ShareMotion
import com.ramwise.lavoz.utils.*
import com.ramwise.lavoz.utils.factories.AdapterViewModelFactory
import com.ramwise.lavoz.utils.factories.CommentFactory
import com.ramwise.lavoz.utils.jodatime.differenceToString
import com.ramwise.lavoz.utils.rx.*
import com.ramwise.lavoz.viewmodels.BaseViewModel
import com.ramwise.lavoz.viewmodels.adapters.MotionListConcludedAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.MotionListOpenAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotionListGenericAdapterViewModel
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotivationsGenericAdapterViewModel

import org.joda.time.DateTime
import org.joda.time.Seconds
import rx.Observable
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * @param forMotionId The motion id for which to show the motivations.
 *
 * @param clickRecyclerView An Observable that emits the position of the cell that was clicked.
 *
 * @param clickAddOrReply An Observable that emits a signal when the user intends to add a
 *                           motivation. The emitted value is the position of the cell that was
 *                           clicked, or null if the user clicked the general add button.
 *                           This determines whether to reply or whether to add a root motivation.
 *
 * @param clickHeartOrTrash An Observable that emits a Pair containing the position of the cell that
 *                          was clicked and a CommentVoteOption constant.
 *
 * @param clickConfirmTrash An Observable that emits the motivation id that should be removed after
 *                          the user has confirmed that this is their desire.
 */
class MotivationsFragmentViewModel(
        forMotionId: Int,
        clickRecyclerView: Observable<Int>,
        clickAddOrReply: Observable<Int>,
        clickHeartOrTrash: Observable<Pair<Int, Int>>,
        clickConfirmTrash: Observable<Int>): BaseViewModel() {

    @Inject lateinit var lavozService: LavozService
    @Inject lateinit var authService: AuthenticationService

    /** Emits the AdapterViewModel objects required by the RecyclerView. */
    val adapterViewModels: Observable<ArrayList<MotivationsGenericAdapterViewModel>>

    /** Emits a signal whenever the user upvotes a motivation. The emitted value is of type Unit.
     *
     * It is important that this Observable is subscribed to, even though there is no UI-change
     * bound to it (since the heart icon already changes upon click).
     */
    val upvoteToAPI: Observable<Unit>

    /** Emits a signal whenever the user removes an upvote on a motivation. The emitted value is of
     * type Unit.
     *
     * It is important that this Observable is subscribed to, even though there is no UI-change
     * bound to it (since the heart icon already changes upon click).
     */
    val upvoteRemoveToAPI: Observable<Unit>

    /** Emits a signal whenever the user deleted a motivation. The emitted value is of
     * type Unit.
     *
     * It is important that this Observable is subscribed to, even though there is no UI-change
     * bound to it (since the heart icon already changes upon click).
     */
    val deleteMotivationToAPI: Observable<Unit>

    /** Emits a signal whenever the user wishes to navigate to the add motivations page but cannot.
     */
    val errorCannotAddMotivation: Observable<Unit>

    /** Emits a boolean indicating whether the add button should be gone (true) or visible (false)
     */
    val addButtonHidden: Observable<Boolean>

    /** Emits a signal whenever the user should navigate to the add motivation page.
     *
     * The value emitted is an [AddMotivationParams] object with the required parameters in it.
     */
    val navigateToAddMotivation: Observable<AddMotivationParams>

    /** Emits a signal whenever the RecyclerView should scroll to a certain position.
     *
     * The emitted value is a Pair of integers representing the section and the relative position
     * within that section.
     */
    val scrollToRowInSection: Observable<Pair<Int, Int>>

    /** Emits a boolean that indicates whether the Activity/Fragment should show/hide its loading
     * spinner in the overlay in x seconds.
     * Any false signal should cancel a timer controller by the Activity/Fragment, any true should
     * activate such a timer.
     */
    val prepareLayoutOverlayHUDLoader: Observable<Boolean>

    /** Emits a boolean that indicates whether the Activity/Fragment should show/hide its loading
     * spinner in the overlay in x seconds, with a "deleting comment" text.
     * Any false signal should cancel a timer controller by the Activity/Fragment, any true should
     * activate such a timer.
     */
    val deletingMotivationOverlayHUDLoader: Observable<Boolean>

    /** Emits a signal when a confirmation dialog should be shown to the user to confirm that they
     * wish to remove the comment. The emitted value is the motivation id to be removed.
     */
    val showConfirmTrashDialog: Observable<Int>

    /** An array of integers of the comment id's that should be visibly expanded. */
    private var includeCommentChildren = ArrayList<Int>()

    /** A hashmap with keys being integers of the comment id's that were given a heart since the
     * last data refresh from the API, and their values the CommentVoteOption constant matching it.
     * This is needed to keep the heart icon correct between re-renders, since we cannot rely on
     * the API data, which gets stale (when it comes to whether something was upvoted or not).
     *
     * Clear this hashmap before (or immediately after) new data from the API comes in.
     */
    private var cachedHeartData = SparseIntArray()

    private val commentFactory = CommentFactory()

    private val responseFactory = LavozResponseFactory()

    init {
        LavozApplication.networkComponent.inject(this)

        val activityIndicator = RxActivityIndicator()

        val deletingMotivationActivityIndicator = RxActivityIndicator()

        prepareLayoutOverlayHUDLoader = activityIndicator.asObservable()

        deletingMotivationOverlayHUDLoader = deletingMotivationActivityIndicator.asObservable()

        /** An internal helper variable. Should emit a signal whenever new motion data should be
         * obtained, i.e. after deleting a comment.
         */
        val refreshData = RxVariable((Unit))

        // MARK: - Cell expand/collapse related

        /** An array that represents the currently visible comments (in flat form).
         * The value of this must be updated when the ArrayList of Comments is actually known.
         */
        var currentFlatResponses: List<Comment> = ArrayList()

        val cellClickedRefresh = clickRecyclerView
                .map {
                    // Introduce a side-effect here.
                    if (it < currentFlatResponses.size) {
                        val comment = currentFlatResponses[it]
                        val idx = includeCommentChildren.indexOf(comment.id)

                        if (idx == -1) {
                            includeCommentChildren.add(includeCommentChildren.size, comment.id)
                        } else {
                            includeCommentChildren.removeAt(idx)
                        }
                    }
                    it
                }
                .startWith(0)
                .share()

        scrollToRowInSection = cellClickedRefresh.map { Pair(1, it) }

        // MARK: - Main data related

        val motionFromAPI = Observable
                .combineLatest(
                        refreshData.asObservable(),
                        ObservableFactory()
                                .engaged(engaged)
                ) { a, b -> (Unit) }
                .flatMap {
                    lavozService
                            .getMotion(forMotionId, true)
                            .subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicator)
                            .onErrorReturn { responseFactory.createError<Motion>(it) }
                }
                .shareReplay(1)
                .retain(retainBag)

        val motionAndMotivations = motionFromAPI
                .map { it.motion }
                .unwrap()
                .map { Pair(it, it.commentsMerged) }
                // it.commentsMerged is a heavy operation, so share the outcome.
                .map {
                    // Introduce a small internal side-effect.
                    // Reset the cached heart data.
                    cachedHeartData = SparseIntArray()
                    it
                }
                .shareReplay(1)

        val motion = motionAndMotivations.map { it.first }
        val motivations = motionAndMotivations.map { it.second }

        adapterViewModels = Observable
                .combineLatest(motionAndMotivations, cellClickedRefresh) {
                    a: Pair<Motion, ArrayList<Comment>?>, b: Int ->
                    val flatList = commentFactory.flatResponses(a.second, includeCommentChildren)
                    if (flatList != null)
                        currentFlatResponses = flatList

                    AdapterViewModelFactory().build(
                            a.first,
                            flatList,
                            authService.authToken!!.user!!.id,
                            cachedHeartData)
                }


        // MARK: - Related to upvotes

        upvoteToAPI = clickHeartOrTrash
                .filter { it.second == CommentVoteOption.UPVOTE }
                .map { it.first }
                .map {
                    if (it != null && it < currentFlatResponses.size)
                        currentFlatResponses[it].id
                    else
                        -1
                }
                .filter { it > -1 }
                .flatMap {
                    // Introduce a small side-effect
                    cachedHeartData.put(it, CommentVoteOption.UPVOTE)

                    // Now prepare the API action
                    lavozService.commentVote(it, CommentVoteOption.UPVOTE)
                            .subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicator)
                            .onErrorReturn { responseFactory.createError<CommentVote>(it) }
                }
                .share()
                .map { (Unit) }

        upvoteRemoveToAPI = clickHeartOrTrash
                .filter { it.second == CommentVoteOption.CLEAR }
                .map { it.first }
                .map {
                    println("Getting it now")
                    println(it)
                    if (it != null && it < currentFlatResponses.size)
                        currentFlatResponses[it].id
                    else
                        -1
                }
                .filter { it > -1 }
                .flatMap {
                    // Introduce a small side-effect
                    cachedHeartData.put(it, CommentVoteOption.CLEAR)

                    // Now prepare the API action
                    lavozService
                            .commentVoteRemoveByCommentId(it, forMotionId)
                            .subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicator)
                            .onErrorReturn { responseFactory.createError<Unit>(it) }
                }
                .share()
                .map { (Unit) }

        // MARK: - Deal with adding/removing motivations

        showConfirmTrashDialog = clickHeartOrTrash
                // CommentVoteOption.UNDEFINED in this context is used for trash actions
                .filter { it.second == CommentVoteOption.UNDEFINED }
                .map { it.first }
                .map {
                    if (it != null && it < currentFlatResponses.size)
                        currentFlatResponses[it].id
                    else
                        -1
                }
                .filter { it > -1 }

        deleteMotivationToAPI = clickConfirmTrash
                .flatMap {
                    lavozService.commentRemove(it)
                            .subscribeOn(Schedulers.io())
                            .trackActivity(deletingMotivationActivityIndicator)
                            .onErrorReturn { responseFactory.createError<Unit>(it) }
                }
                .map {
                    // Trigger a refersh of data
                    refreshData.value = (Unit)
                    // Emit an empty signal.
                    (Unit)
                }
                .share()

        addButtonHidden = motion.map { it.status != MotionStatus.OPEN || it.vote == null}

        val addMotivationIntent = clickAddOrReply
                .map {
                    if (it != null && it < currentFlatResponses.size)
                        currentFlatResponses[it]
                    else
                        null
                }
                .withLatestFrom(motion) { replyToComment: Comment?, motion: Motion ->
                    if (motion.vote == null && replyToComment == null) {
                        // Cannot add a motivation at this time.
                        null
                    } else {
                        AddMotivationParams(
                                motion.id,
                                motion.labelFor ?: "",
                                motion.labelAgainst ?: "",
                                replyToComment?.rootVoteOption ?: motion.vote!!.ballot,
                                replyToComment?.id,
                                replyToComment?.author?.name)
                    }
                }
                .share()

        errorCannotAddMotivation = addMotivationIntent.filter { it == null }.map { (Unit) }

        navigateToAddMotivation = addMotivationIntent.unwrap()
    }


    /** Can be called by the Fragment to ensure that a particular cell will be expanded on the next
     * render.
     */
    fun ensureCellExpands(commentId: Int) {
        val idx = includeCommentChildren.indexOf(commentId)
        if (idx == -1) {
            includeCommentChildren.add(includeCommentChildren.size, commentId)
        }
    }

    /** A simple data class that represents all that should be passed on to the Add Motivation page.
     *
     * While this is closely related to various models, the purpose of this data class
     * is to provide an abstraction that respects the MVVM pattern. Unlike regular models,
     * we can expose [AddMotivationParams] to the Fragment.
     *
     * For the meaning of these parameters, see [AddMotivationFragmentViewModel]
     */
    data class AddMotivationParams(val forMotionId: Int, val motionLabelFor: String,
                                   val motionLabelAgainst: String, val rootVoteOption: Int,
                                   val replyToCommentId: Int?, val replyToAuthorName: String?)
}
package com.ramwise.lavoz.viewmodels.fragments

import android.support.v4.content.ContextCompat
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.utils.factories.LavozResponseFactory
import com.ramwise.lavoz.utils.factories.ObservableFactory
import com.ramwise.lavoz.models.Motion
import com.ramwise.lavoz.models.constants.MotionStatus

import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.network.LavozService

import javax.inject.Inject
import com.ramwise.lavoz.utils.factories.AdapterViewModelFactory
import com.ramwise.lavoz.utils.rx.*
import com.ramwise.lavoz.viewmodels.BaseViewModel
import com.ramwise.lavoz.viewmodels.adapters.interfaces.MotionListGenericAdapterViewModel

import rx.Observable
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * @param viewType A MotionListViewType constant.
 *
 * @param clickRecyclerView An Observable that emits a Pair of the position of the cell that was
 *                          clicked, and a MotionStatus constant value of the cell.
 *
 * @param bottomReachedRecyclerView An Observable that emits a signal whenever the bottom of the
 *                                  recyclerView is reached.
 */
class MotionListFragmentViewModel(
        viewType: Int,
        clickRecyclerView: Observable<Pair<Int, Int>>,
        bottomReachedRecyclerView: Observable<Unit>): BaseViewModel() {

    @Inject lateinit var lavozService: LavozService
    @Inject lateinit var authService: AuthenticationService

    /** Emits the AdapterViewModel objects required by the RecyclerView. */
    val adapterViewModels: Observable<ArrayList<MotionListGenericAdapterViewModel>>

    /** Emits a signal whenever the user should be navigated to the motion page.
     * The value emitted is the motion id the target VC should use.
     */
    val navigateToHome: Observable<Int>

    /** Emits a signal whenever the user should be navigated to the home page.
     * The value emitted is the motion id the target VC should use.
     */
    val navigateToMoreDetails: Observable<Int>

    /** Emits a boolean that indicates whether the Activity/Fragment should show/hide its loading
     * spinner in the overlay in x seconds.
     * Any false signal should cancel a timer controller by the Activity/Fragment, any true should
     * activate such a timer.
     */
    val prepareLayoutOverlayHUDLoader: Observable<Boolean>

    /** Emits a signal whenever the refresh icon should be hidden or shown */
    val bottomPageHUDLoader: Observable<Boolean>

    private val responseFactory = LavozResponseFactory()

    init {
        /** The number of items (motions) that should be requested on each call to the API.
         * API allows no more than 20 each time.
         *
         * IMPORTANT: right now, the API is hard-coded to 7, so make sure to follow this number.
         */
        val loadItemsPerRequest = 7

        /** The current page in the paginated table, i.e. the number of times more data has been
         *  successfully reloaded.
         */
        var currentPage = 0

        /** Indicates whether the actual bottom of the paginated view has been reached. */
        var depletedItems = false

        /** A boolean indicating whether any items have loaded in the tableView already. */
        var firstRequestCompleted = false

        LavozApplication.networkComponent.inject(this)

        val activityIndicator = RxActivityIndicator()

        prepareLayoutOverlayHUDLoader = activityIndicator.asObservable()

        val motionsAPIRequestWithOffset = Observable.merge(
                    ObservableFactory()
                            .engagedInterval(engaged)
                            .map { 0 },
                    bottomReachedRecyclerView
                            .filter { (!depletedItems && firstRequestCompleted) }
                            .map { loadItemsPerRequest * currentPage }
                )
                .withLatestFrom(activityIndicator.asObservable(), { offset: Int, currentlyActive: Boolean ->
                    if (currentlyActive) null else offset
                })
                .unwrap()
                .shareReplay(1)

        val motionsFromAPI = motionsAPIRequestWithOffset
                .flatMap {
                    lavozService
                            .getMotions(viewType, it, loadItemsPerRequest)
                            .subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicator)
                            .onErrorReturn { responseFactory.createError<List<Motion>>(it) }
                }
                .shareReplay(1)
                .retain(retainBag)

        val motions = motionsFromAPI
                .map { it.motions }
                .unwrap()
                .withLatestFrom(motionsAPIRequestWithOffset) { motionsList: List<Motion>, offset: Int ->
                    // Try to determine the offset that should be used in future requests.
                    // Do this by looking at the offset of the latest request. If the offset was
                    // higher than 0, increase the pagination. Otherwise, this was likely a refresh
                    // of the latest motion data, in which case the offset should not be increased.
                    // Note that this might yield the wrong outcome if two requests are fired in
                    // rapid succession, where one is a refresh and the other is a request for older
                    // data.
                    // Also note that this is not an ideal place to perform this (stateful) action,
                    // but it suffices for now. The slight side-effects are acceptable.
                    firstRequestCompleted = true

                    if (offset > 0 || currentPage == 0) {
                        currentPage += 1
                        depletedItems = motionsList.size < loadItemsPerRequest
                    }
                    Pair(motionsList, offset)
                }
                .scan(ArrayList<Motion>(), { lastSlice: List<Motion>, newValue: Pair<List<Motion>, Int> ->
                    val (motionsList, offset) = newValue

                    if (offset == 0) {
                        motionsList.filter { obj ->
                            lastSlice.filter { it.id == obj.id}.isEmpty()
                        } + lastSlice
                    } else {
                        lastSlice + motionsList.filter { obj ->
                            lastSlice.filter { it.id == obj.id}.isEmpty()
                        }
                    }
                })
                .shareReplay(1)
                .retain(retainBag)

        val updateUITimerInterval = Observable
                .interval(60, TimeUnit.SECONDS)
                .startWith(0)
                .map { (Unit) }
                .shareReplay(1)

        adapterViewModels = motions
                .map {
                     AdapterViewModelFactory().build(it, updateUITimerInterval)
                }

        navigateToHome = clickRecyclerView
                .filter { it.second == MotionStatus.OPEN }
                .map { it.first }
                .withLatestFrom(motions, { clickedRow: Int, motions: List<Motion> ->
                    if (clickedRow < motions.size)
                        motions[clickedRow].id
                    else
                        -1
                })
                .filter { it > -1 }

        navigateToMoreDetails = clickRecyclerView
                .filter { it.second == MotionStatus.CONCLUDED }
                .map { it.first }
                .withLatestFrom(motions, { clickedRow: Int, motions: List<Motion> ->
                    if (clickedRow < motions.size)
                        motions[clickedRow].id
                    else
                        -1
                })
                .filter { it > -1 }


        bottomPageHUDLoader = Observable
                .merge(
                        activityIndicator.asObservable().filter { !it },
                        motionsAPIRequestWithOffset.map { it > 0 }
                )
    }
}
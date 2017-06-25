package com.ramwise.lavoz.viewmodels.fragments

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.util.Log
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.LavozConstants
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


class AdviceAidFragmentViewModel(isCompleted: Boolean, mainTap: Observable<Unit>): BaseViewModel() {

    @Inject lateinit var lavozService: LavozService
    @Inject lateinit var authService: AuthenticationService

    /** Emits a boolean that indicates whether the Activity/Fragment should show/hide its loading
     * spinner in the overlay in x seconds.
     * Any false signal should cancel a timer controller by the Activity/Fragment, any true should
     * activate such a timer.
     */
    val prepareLayoutOverlayHUDLoader: Observable<Boolean>

    /** Emits a signal whenever the user should navigate to the next page.
     * If false, the user should be navigated to the advice aid page,
     * if true, the user should be shown the party points results.
     */
    val navigateToNext: Observable<Boolean>

    /** The text to appear in the main textfield */
    val mainText: Observable<String>

    /** Emits the text label for the main button */
    val buttonText: Observable<String>


    private val responseFactory = LavozResponseFactory()

    init {
        LavozApplication.networkComponent.inject(this)

        val activityIndicator = RxActivityIndicator()

        prepareLayoutOverlayHUDLoader = activityIndicator.asObservable()

        navigateToNext = mainTap
                .map {
                    val wasPreviouslyCompleted = LavozApplication.shared.getBoolean(LavozConstants.SHARED_KEY_ADVICEAID_COMPLETED, false)

                    if (isCompleted) {
                        LavozApplication.shared
                                .edit()
                                .putBoolean(LavozConstants.SHARED_KEY_ADVICEAID_COMPLETED, true)
                                .apply()
                        true
                    } else if (wasPreviouslyCompleted) {
                        true
                    } else {
                        LavozApplication.shared
                                .edit()
                                .putBoolean(LavozConstants.SHARED_KEY_ADVICEAID_STARTED, true)
                                .apply()
                        false
                    }
                }

        mainText = ObservableFactory()
                .engaged(engaged)
                .map {
                    val wasPreviouslyCompleted = LavozApplication.shared.getBoolean(LavozConstants.SHARED_KEY_ADVICEAID_COMPLETED, false)

                    if (isCompleted || wasPreviouslyCompleted) {
                        resources.getString(R.string.adviceaid_completed)
                    } else {
                        resources.getString(R.string.adviceaid_welcome)
                    }
                }

        buttonText = ObservableFactory()
                .engaged(engaged)
                .map {
                    val wasPreviouslyCompleted = LavozApplication.shared.getBoolean(LavozConstants.SHARED_KEY_ADVICEAID_COMPLETED, false)

                    if (isCompleted || wasPreviouslyCompleted) {
                        resources.getString(R.string.adviceaid_view_partypoints)
                    } else {
                        if (!LavozApplication.shared.getBoolean(LavozConstants.SHARED_KEY_ADVICEAID_STARTED, false)) {
                            resources.getString(R.string.adviceaid_start)
                        } else {
                            resources.getString(R.string.adviceaid_continue)
                        }
                    }
                }
    }
}
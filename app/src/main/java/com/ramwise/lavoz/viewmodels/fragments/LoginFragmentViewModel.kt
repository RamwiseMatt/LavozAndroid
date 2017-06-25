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
import com.ramwise.lavoz.models.AuthToken
import com.ramwise.lavoz.models.Vote
import com.ramwise.lavoz.models.constants.HttpCode
import com.ramwise.lavoz.utils.sharing.ShareMotion
import com.ramwise.lavoz.utils.*
import com.ramwise.lavoz.utils.rx.*
import com.ramwise.lavoz.viewmodels.BaseViewModel

import org.joda.time.DateTime
import org.joda.time.Seconds
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func2
import rx.functions.Func3
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit


class LoginFragmentViewModel(
        fbAccessToken: Observable<String?>,
        googleAuthCode: Observable<String?>): BaseViewModel() {

    @Inject lateinit var lavozService: LavozService
    @Inject lateinit var authService: AuthenticationService

    /** Emits a boolean that indicates whether the Activity/Fragment should show/hide its loading
     * spinner in the overlay in x seconds.
     * Any false signal should cancel a timer controller by the Activity/Fragment, any true should
     * activate such a timer.
     */
    val prepareLayoutOverlayHUDLoader: Observable<Boolean>

    /** Emits all native [AuthToken]s that get created via this view model. */
    val authToken: Observable<AuthToken?>

    private val responseFactory = LavozResponseFactory()

    init {
        LavozApplication.networkComponent.inject(this)

        val activityIndicator = RxActivityIndicator()

        prepareLayoutOverlayHUDLoader = activityIndicator.asObservable()

        val nativeToken: Observable<AuthToken> = Observable.never()

        val loginFacebookToAPI = fbAccessToken
                .withLatestFrom(authService.authTokenAsObservable(), { tokenString: String?,
                                                nativeToken: AuthToken? ->
                    // Return null if there is an existing native token already.
                    // This null will then get filtered out via unwrap().
                    if (nativeToken != null) null else tokenString
                })
                .distinctUntilChanged()
                .unwrap()
                .flatMap {
                    lavozService
                            .loginFacebookUser(it)
                            .subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicator)
                            .onErrorReturn { responseFactory.createError<AuthToken>(it) }
                }
                .shareReplay(1)

        val loginGoogleToAPI = googleAuthCode
                .withLatestFrom(authService.authTokenAsObservable(), { authCodeString: String?,
                                                                       nativeToken: AuthToken? ->
                    // Return null if there is an existing native token already.
                    // This null will then get filtered out via unwrap().
                    if (nativeToken != null) null else authCodeString
                })
                .distinctUntilChanged()
                .unwrap()
                .flatMap {
                    lavozService
                            .loginGoogleUser(it)
                            .subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicator)
                            .onErrorReturn { responseFactory.createError<AuthToken>(it) }
                }
                .shareReplay(1)

        authToken = Observable.merge(
                loginFacebookToAPI.map { it.authToken },
                loginGoogleToAPI.map { it.authToken }
        )
    }
}
package com.ramwise.lavoz.viewmodels.activities

import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.network.LavozService
import com.ramwise.lavoz.utils.factories.LavozResponseFactory
import com.ramwise.lavoz.utils.rx.RxActivityIndicator
import com.ramwise.lavoz.viewmodels.BaseViewModel
import rx.Observable
import javax.inject.Inject


class LoginActivityViewModel: BaseViewModel() {

    @Inject lateinit var lavozService: LavozService
    @Inject lateinit var authService: AuthenticationService

    /** Emits a signal when the user should be redirected to the login activity. This signal is
     * passed whether the API call succeeded or not.
     *
     * Side-effect: The auth token is cleaned from disk when this Observable is subscribed to.
     */
    val navigateToHomeActivity: Observable<Unit>

    private val responseFactory = LavozResponseFactory()

    init {
        LavozApplication.networkComponent.inject(this)

        navigateToHomeActivity = authService
                .authTokenAsObservable()
                .filter { it != null }
                .map { (Unit) }
    }
}
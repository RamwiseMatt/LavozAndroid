package com.ramwise.lavoz.viewmodels.activities

import com.google.firebase.iid.FirebaseInstanceId
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.LavozConstants
import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.network.LavozService
import com.ramwise.lavoz.utils.factories.LavozResponseFactory
import com.ramwise.lavoz.utils.factories.ObservableFactory
import com.ramwise.lavoz.utils.rx.*
import com.ramwise.lavoz.viewmodels.BaseViewModel
import rx.Observable
import rx.schedulers.Schedulers
import javax.inject.Inject


class HomeActivityViewModel(clickSignOut: Observable<Unit>): BaseViewModel() {

    @Inject lateinit var lavozService: LavozService
    @Inject lateinit var authService: AuthenticationService

    /** Emits the full name of the authenticated user */
    val userFullName: Observable<String>

    /** Emits true when the user has just clicked the sign out button */
    val prepareForSignOut: Observable<Unit>

    /** Emits a signal when the user should be redirected to the login activity. This signal is
     * passed whether the API call succeeded or not.
     *
     * Side-effect: The auth token is cleaned from disk when this Observable is subscribed to.
     */
    val navigateToLoginActivity: Observable<Unit>

    /** Emits a signal when all tokens should be removed via the AuthenticationService. */
    val removeAuthToken: Observable<Unit>

    /** Should always be observed by the Activity. Updates the registration token to the server
     * if needed.
     */
    val updatePushTokenIfNeeded: Observable<Unit>

    /** Emits a boolean that indicates whether the Activity/Fragment should show/hide its loading
     * spinner in the overlay in x seconds.
     * Any false signal should cancel a timer controller by the Activity/Fragment, any true should
     * activate such a timer.
     */
    val prepareLayoutOverlayHUDLoader: Observable<Boolean>

    private val responseFactory = LavozResponseFactory()

    init {
        LavozApplication.networkComponent.inject(this)

        val activityIndicator = RxActivityIndicator()

        prepareLayoutOverlayHUDLoader = activityIndicator.asObservable()

        userFullName = authService
                .authTokenAsObservable()
                .map { it?.user?.name }
                .unwrap()

        prepareForSignOut = clickSignOut


        val signOutToAPI = clickSignOut
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

        // MARK: - Registration token related

        val registrationTokenToAPI = Observable
                .just((Unit))
                .map {
                    val new = FirebaseInstanceId.getInstance().token
                    val stored: String? = LavozApplication.shared.getString(
                            LavozConstants.SHARED_KEY_REGISTRATION_TOKEN, null)

                    if (new != null && (stored == null || new != stored)) {
                        LavozApplication.shared
                                .edit()
                                .putString(LavozConstants.SHARED_KEY_REGISTRATION_TOKEN, new)
                                .apply()
                        new
                    } else {
                        null
                    }
                }
                .filter { it != null }
                .flatMap {
                    lavozService.enablePushNotifications(it!!)
                            .subscribeOn(Schedulers.io())
                            .trackActivity(activityIndicator)
                            .onErrorReturn { responseFactory.createError<Unit>(it) }
                }
                .shareReplay(1)
                .retain(retainBag)

        updatePushTokenIfNeeded = registrationTokenToAPI
                .map {
                    if (it.err != null) {
                        LavozApplication.shared
                                .edit()
                                .remove(LavozConstants.SHARED_KEY_REGISTRATION_TOKEN)
                                .apply()
                    }
                    (Unit)
                }
    }
}
package com.ramwise.lavoz.utils.factories

import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.network.AuthenticationService
import com.ramwise.lavoz.utils.rx.RxVariable
import rx.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ObservableFactory {
    @Inject lateinit var authService: AuthenticationService

    init {
        LavozApplication.networkComponent.inject(this)
    }

    /** Creates an observable that emits a signal whenever the provided engaged parameter
    * emits a 'true' boolean (usually indicating the fragment/activity became active).
    *
    * @param engaged An RxVariable that holds a value indicating whether the caller (ViewModel) is engaged.
    *                This has an affect on both when signals are emitted and how they are filtered.
    *
    * @param ignoreAnonymous A boolean indicating whether to ignore all signals when the user is not authenticated.
    *
    * @return An Observable.
    */
    fun engaged(engaged: RxVariable<Boolean>, ignoreAnonymous: Boolean = true): Observable<Unit> {
        return engaged
                .asObservable()
                .filter { (it == true) && (!ignoreAnonymous || authService.authToken != null) }
                .map { (Unit) }
    }

    /**
     * Creates an observable that emits a signal periodically, and also whenever the provided engaged parameter
     * emits a 'true' boolean (usually indicating the fragment/activity became active).
     * However, these signals will be grouped in rxJava windows, where each window will only emit a single
     * signal. This ensures that, if the interval signal is emitted when the engaged variable is currently false,
     * a signal will be emitted immediately after the engaged variable becomes true, rather than waiting for the
     * next interval signal to arrive.
     *
     * Important: Even if no signals are being emitted, heat gets generated as long as a subscription exists.
     *
     * @param engaged An RxVariable that holds a value indicating whether the caller (ViewModel) is engaged.
     *                    This has an affect on both when signals are emitted and how they are filtered.
     * @param seconds The interval size. Needs to be at least 10 seconds.
     * @param ignoreAnonymous A boolean indicating whether to ignore all signals when the user is not authenticated.
     *
     * @return An Observable.
     */
    fun engagedInterval(engaged: RxVariable<Boolean>, seconds: Long = 600,
                        ignoreAnonymous: Boolean = true): Observable<Unit> {
        return Observable
                .merge(
                        // Include the engaged Variable (as an observable), mapping to Int 0 for compatibility.
                        engaged.asObservable().map { 0 },
                        // Include an interval of specified size
                        Observable
                                .interval(seconds, TimeUnit.SECONDS)
                                // Delay the interval by 0.1 seconds to ensure it always fits into the window (defined later).
                                .delaySubscription(100, TimeUnit.MILLISECONDS)
                )
                // Filter out all signals when the ViewModel is not engaged (i.e. activity not in view)
                .filter { engaged.value && (!ignoreAnonymous || authService.authToken != null) }
                .map { (Unit) }
                // Define a window in which all signals by the above observables will fit. Same size as interval above.
                .window(seconds, TimeUnit.SECONDS, 999)
                // Only allow the first signal that comes out of the window. The rest will be ignored.
                .flatMap {
                    it.take(1)
                }
    }
}
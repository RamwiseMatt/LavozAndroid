package com.ramwise.lavoz.viewmodels

import android.content.res.Resources
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.utils.rx.RxVariable
import com.ramwise.lavoz.utils.rx.addDisposableTo
import rx.Observable
import rx.subscriptions.CompositeSubscription

/** The class that all ViewModels for Activities and Fragments should inherit from.
 *
 * Note that ViewModels for Adapters should inherit from [BaseAdapterViewModel].
 */
abstract class BaseViewModel {
    /** Indicates whether the activity/fragment it belongs to is active or not.
     *
     * The value of this should be set from the relevant activity/fragment during their lifecycle by
     * using the engage() and disengage() methods.
     */
    val engaged = RxVariable(false)

    /** A shortcut for getResources() within the application's context. This is useful because
     * ViewModels often need acccess to such things as resource strings.
     */
    val resources: Resources
        get() {
            return LavozApplication.context.resources
        }


    /** Due to the nature of Fragments, it is sometimes desired to have a ViewModel retain a
     * Subscriber to its API observables, such that refCount() will not let go of the Observable and
     * fire the API call again.
     * Such subscriptions have no value to the Fragment/Activity and should be kept private.
     * An Observable should only be subscribed to in a ViewModel when absolutely necessary (as a
     * rule, ViewModels exist only for defining Observables, not subscribing to them).
     *
     * The retainBag is cleared when the ViewModel's onDestroy() method is called, so remember to
     * do call that method from the Fragment/Activity's onDestroy() method.
     */
    protected val retainBag = CompositeSubscription()

    /** Reflects that the corresponding activity/fragment is active. The ViewModel should act
     * in accordance with this state now.
     */
    fun engage() {
        engaged.value = true
    }

    /** Reflects that the corresponding activity/fragment is no longer active. The ViewModel should
     *  act in accordance with this state now.
     */
    fun disengage() {
        engaged.value = false
    }

    fun destroy() {
        retainBag.clear()
    }

    /** In some cases it is desirable for the ViewModel to ensure the existence of a Subscriber to
     * an Observable, even when the relevant Fragment/Activity does not subscribe to it. A good
     * example of this is an API-call Observable.
     *
     * This helper method creates a Subscription to an Observable and retains it until the end of
     * the ViewModel's lifecycle.
     *
     * @see [retainBag]
     * @see [Observable.retain]
     */
    fun <T> retainObservable(obs: Observable<T>) {
        obs.subscribe().addDisposableTo(retainBag)
    }
}
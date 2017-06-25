package com.ramwise.lavoz.utils.rx

import com.jakewharton.rxbinding.view.clicks
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

/** A convenience method based on an RxSwift pattern. Rather than wrapping the entire chain of
 * Observable operators in a CompositeSubscription().add() call, simply add the Disposable to the
 * bag via another chain call.
 *
 * @param bag The CompositeSubscription object to which the Subscription should be added.
 */
fun Subscription.addDisposableTo(bag: CompositeSubscription) {
    bag.add(this)
}

/** Takes a sequence of optional elements and returns a sequence of non-optional elements,
 * filtering out any null values.
 *
 * @return An Observable sequence of non-optional elements
 */
fun <T> Observable<T?>.unwrap(): Observable<T> {
    return this
            .filter { it != null }
            .map { it!! }
}

/** Ensures that the Observable emits its latest value, as long as there has been at least one
 * subscriber since that last value was emitted.
 *
 * This is a shortcut for .replay().refCount().
 *
 * @param count The number of elements to replay.
 */
fun <T> Observable<T>.shareReplay(count: Int): Observable<T> {
    return this
            .replay(count)
            .refCount()
}

/** Ensures that the Observable is subscribed to and remains subscribed to for the duration of
 * a the lifetime of the retainBag provided. This is especially useful in combination with
 * [shareReplay], when you want to ensure an Observable can replay the latest value that existed,
 * even when no Fragment/Activity-defined subscribers exist
 * (e.g. when a Fragment is in the backstack)
 *
 * @param retainBag The CompositeSubscription object to which the subscription should be added.
 */
fun <T> Observable<T>.retain(retainBag: CompositeSubscription): Observable<T> {
    retainBag.add(this.subscribe())

    return this
}

/** A shortcut method for observing Observables on the main UI thread. */
fun <T> Observable<T>.ui(): Observable<T> {
    return this.observeOn(AndroidSchedulers.mainThread())
}
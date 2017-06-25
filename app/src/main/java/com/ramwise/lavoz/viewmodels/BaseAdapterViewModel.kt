package com.ramwise.lavoz.viewmodels

import android.content.res.Resources
import com.ramwise.lavoz.LavozApplication
import com.ramwise.lavoz.utils.rx.RxVariable
import com.ramwise.lavoz.utils.rx.addDisposableTo
import rx.Observable
import rx.subscriptions.CompositeSubscription

/** The class that all ViewModels for Adapters should inherit from. */
abstract class BaseAdapterViewModel {

    /** A shortcut for getResources() within the application's context. This is useful because
     * ViewModels often need acccess to such things as resource strings.
     */
    val resources: Resources
        get() {
            return LavozApplication.context.resources
        }
}
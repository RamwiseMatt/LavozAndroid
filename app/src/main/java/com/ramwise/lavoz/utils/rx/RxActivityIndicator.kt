package com.ramwise.lavoz.utils.rx

import rx.Observable
import java.util.concurrent.locks.ReentrantLock


class RxActivityIndicator {
    private val _lock = ReentrantLock()
    private val _variable = RxVariable(0)
    private val _loading: Observable<Boolean>

    init {
        _loading = _variable.asObservable().map { it > 0 }.distinctUntilChanged()
    }

    fun <T> trackActivityOfObservable(source: Observable<T>): Observable<T> {
        return Observable.using({ this.increment(); null }, { source }, { this.decrement() })
    }

    private fun increment() {
        _lock.lock()
        _variable.value = _variable.value + 1
        _lock.unlock()
    }

    private fun decrement() {
        _lock.lock()
        _variable.value = Math.max(0, _variable.value - 1)
        _lock.unlock()
    }

    fun asObservable(): Observable<Boolean> {
        return _loading
    }
}

fun <T> Observable<T>.trackActivity(activityIndicator: RxActivityIndicator): Observable<T> {
    return activityIndicator.trackActivityOfObservable(this)
}
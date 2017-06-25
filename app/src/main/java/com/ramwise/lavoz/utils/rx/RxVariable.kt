package com.ramwise.lavoz.utils.rx

import rx.Observable
import rx.subjects.BehaviorSubject


/** A thin wrapper around RxJava's BehaviorSubject. This is based on RxSwift's Variable. */
class RxVariable<T>(value: T) {

    private var _lock = Any()

    private var _value = value

    private val _subject = BehaviorSubject.create(value)

    var value : T
        get() {
            synchronized(_lock) {
                return _value
            }
        }
        set(v) {
            synchronized(_lock) {
                _value = v

                _subject.onNext(v)
            }
        }

    fun asObservable() : Observable<T> {
        return _subject
    }

    protected fun finalize() {
        _subject.onCompleted()
    }
}
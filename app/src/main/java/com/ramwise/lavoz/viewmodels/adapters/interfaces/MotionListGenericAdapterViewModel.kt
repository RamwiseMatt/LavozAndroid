package com.ramwise.lavoz.viewmodels.adapters.interfaces


interface MotionListGenericAdapterViewModel {
    /** Should return a MotionStatus constant */
    val type: Int

    /** Should return a unique ID that can be used by the adapter to identify this data */
    val id: Int
}
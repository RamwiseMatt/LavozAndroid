package com.ramwise.lavoz.viewmodels.adapters.interfaces


interface MotivationsGenericAdapterViewModel {
    /** Should return a MotivationsViewType constant */
    val type: Int

    /** Should return a unique ID that can be used by the adapter to identify this data.
     *
     * The header can use id=0. Motivations themselves can use the comment id.
     */
    val id: Int
}
package com.ramwise.lavoz.utils.android

import java.util.*

/** @return A simple string of 32 characters */
fun String.Companion.randomize() : String {
    return UUID.randomUUID().toString().replace("-", "")
}

/** Creates a new ArrayList based on a given List */
fun <T> List<T>.toArrayList() : ArrayList<T> {
    return ArrayList(this)
}

/** Zips two ArrayLists in such a way that all elements end up in the returned array, i.e. the
 * longer ArrayList just ends up not being zipped by its counterparts elements near the end.
 *
 * @param other The ArrayList to zip. If null is provided, the original array is returned.
 */
fun <T> ArrayList<T>.merge(other: ArrayList<T>?) : ArrayList<T> {
    if (other == null) return this

    val c1_ = this.indices.max()
    val c2_ = other.indices.max()

    if (c1_ == null) return other
    if (c2_ == null) return this

    val c1 = c1_ + 1
    val c2 = c2_ + 1

    val cMin = Math.min(c1, c2)

    val indices =       if (c1 >= c2) this.indices else other.indices
    val (long, short) = if (c1 >= c2) Pair(this, other) else Pair(other, this)

    val result = ArrayList<T>(c1 + c2)
    for (i in indices) {
        result.add(long[i])
        if (i < cMin) result.add(short[i])
    }

    return result
}

/** Zips two ArrayLists in such a way that all elements end up in the returned array, i.e. the
 * longer ArrayList just ends up not being zipped by its counterparts elements near the end.
 *
 * Unlike the regular merge extension, this one includes an integer identifier with each element,
 * which can be used to identify from which of the two ArrayLists the element came from.
 *
 * @param other The ArrayList to zip. If null is provided, the original array is returned with each
 *              element paired with the correct identifier.
 *
 * @param id1 The identifier for the original ArrayList
 *
 * @param id2 The identifier for the other ArrayList (i.e. the one passed as a parameter)
 */
fun <T> ArrayList<T>.merge(other: ArrayList<T>?, id1: Int, id2: Int) : ArrayList<Pair<T, Int>> {
    if (other == null) return this.map { Pair(it, id1) }.toArrayList()

    val c1 = this.indices.max()
    val c2 = other.indices.max()

    if (c1 == null) return this.map { Pair(it, id2) }.toArrayList()
    if (c2 == null) return this.map { Pair(it, id1) }.toArrayList()

    val cMin = Math.min(c1, c2)

    val indices =           if (c1 >= c2) this.indices else other.indices
    val (long, short) =     if (c1 >= c2) Pair(this, other) else Pair(other, this)
    val (longId, shortId) = if (c1 >= c2) Pair(id1, id2) else Pair(id2, id1)

    val result = ArrayList<Pair<T, Int>>(c1 + c2)
    for (i in indices) {
        result.add(Pair(long[i], longId))
        if (i < cMin) result.add(Pair(short[i], shortId))
    }

    return result
}
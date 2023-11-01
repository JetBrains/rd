package com.jetbrains.rd.util.hash

class IncrementalHash64(r: Long = 0) {
    fun mix(mix: String?) = IncrementalHash64(mix?.fold(result) { acc, c -> acc*31 + c.toInt()} ?: result)
    fun<T> mix(collection : Iterable<T>, map : (T) -> String) = collection.fold(this) {acc, elt -> acc.mix(map(elt))}
    val result: Long = r
}


//PLEASE DO NOT CHANGE IT!!! IT'S EXACTLY THE SAME ON C# SIDE
fun String?.getPlatformIndependentHash(initial: Long = 19L) : Long = this?.fold(initial) { acc, c -> acc*31 + c.toInt()} ?:0
fun Int.getPlatformIndependentHash(initial: Long = 19L) : Long = initial*31 + (this + 1)
fun Long.getPlatformIndependentHash(initial: Long = 19L) : Long = initial*31 + (this + 1)
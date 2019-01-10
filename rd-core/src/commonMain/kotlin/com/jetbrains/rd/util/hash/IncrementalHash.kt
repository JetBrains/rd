package com.jetbrains.rd.util.hash

class IncrementalHash64(r: Long = 0) {
    fun mix(mix: String?) = IncrementalHash64(mix?.fold(result) { acc, c -> acc*31 + c.toInt()} ?: result)
    fun<T> mix(collection : Iterable<T>, map : (T) -> String) = collection.fold(this) {acc, elt -> acc.mix(map(elt))}
    val result: Long = r
}
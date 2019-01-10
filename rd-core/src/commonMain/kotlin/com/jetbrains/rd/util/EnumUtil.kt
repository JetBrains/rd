package com.jetbrains.rd.util

inline fun <reified T:Enum<T>> parseFromOrdinal(ordinal: Int) : T {
    enumValues<T>().let {  values ->
        require (ordinal in 0..values.lastIndex) {"'$ordinal' not in range of ${T::class.simpleName} values: [0..${values.lastIndex}]"}
        return values[ordinal]
    }
}
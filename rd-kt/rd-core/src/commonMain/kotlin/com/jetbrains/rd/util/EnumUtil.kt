package com.jetbrains.rd.util

inline fun <reified T:Enum<T>> parseFromOrdinal(ordinal: Int) : T {
    enumValues<T>().let {  values ->
        require (ordinal in 0..values.lastIndex) {"'$ordinal' not in range of ${T::class.simpleName} values: [0..${values.lastIndex}]"}
        return values[ordinal]
    }
}

inline fun <reified T:Enum<T>> parseFromFlags(flags: Int) : EnumSet<T> {
    enumValues<T>().let { values ->
        require(flags in 0 until (1 shl values.size)) {"'$flags' not in range of ${T::class.simpleName} enum set: [0..${(1 shl values.size)})"}
        val res = mutableSetOf<T>()
        var x = flags
        var i = 0
        while (x > 0) {
            if (x % 2 == 1)
                res.add(values[i])
            i++
            x /= 2
        }
        return enumSetOf(res)
    }
}


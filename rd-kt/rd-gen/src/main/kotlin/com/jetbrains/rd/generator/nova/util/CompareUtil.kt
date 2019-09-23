package com.jetbrains.rd.generator.nova.util

fun <T : Comparable<*>> compareLists(list1: List<Comparable<T>>, list2: List<Comparable<T>>): Int {
    if (list1.size == list2.size) {
        for (i in list1.indices) {
            compareValues(list1[i], list2[i]).let {
                if (it != 0) return it
            }
        }
    }
    return compareValues(list1.size, list2.size)
}
package com.jetbrains.rider.util

import java.util.*

enum class OfEqualItems {
    TakeAny,
    TakeFirst,
    TakeLast
}

fun <T> List<T>.binSearch(element: T, comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size,
                          which: OfEqualItems = OfEqualItems.TakeAny): Int {
    rangeCheck(size, fromIndex, toIndex)
    var left = fromIndex
    var right = fromIndex + toIndex - 1
    if (!(left <= right))
        return -(0 + 1)

    when (which) {
        OfEqualItems.TakeAny -> {
            while (left <= right) {
                val median = (left + right).ushr(1)
                val diff = comparator.compare(this[median], element)
                if (diff == 0) {
                    return median
                }
                if (diff < 0)
                    left = median + 1
                else
                    right = median - 1
            }
            return -(left + 1)
        }
        OfEqualItems.TakeFirst -> {
            while (left < right) {
                val median = (left + right).ushr(1)
                val diff = comparator.compare(this[median], element)
                if (diff < 0)
                    left = median + 1;
                else
                    right = median;
            }
            val lastDiff = comparator.compare(this[left], element)
            if (lastDiff == 0)
                return left
            else if (lastDiff > 0)
                return -(left + 1)
            else
                return -(left + 1 + 1)
        }
        OfEqualItems.TakeLast -> {
            while (left < right) {
                val median = (left + right + 1).ushr(1);
                val diff = comparator.compare(this[median], element)
                if (diff > 0)
                    right = median - 1;
                else
                    left = median;
            }
            val lastDiff = comparator.compare(this[left], element);
            if (lastDiff == 0)
                return left
            else if (lastDiff > 0)
                return -(left + 1)
            else
                return -(left + 1 + 1)
        }
    }
}

private fun rangeCheck(size: Int, fromIndex: Int, toIndex: Int) {
    when {
        fromIndex > toIndex -> throw IllegalArgumentException("fromIndex ($fromIndex) is greater than toIndex ($toIndex).")
        fromIndex < 0 -> throw IndexOutOfBoundsException("fromIndex ($fromIndex) is less than zero.")
        toIndex > size -> throw IndexOutOfBoundsException("toIndex ($toIndex) is greater than size ($size).")
    }
}
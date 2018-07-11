package com.jetbrains.rider.rdtext

import com.jetbrains.rider.framework.impl.Range
import com.jetbrains.rider.framework.impl.isEmpty

enum class DiffChangeKind {
    EQUAL,
    INSERTED,
    DELETED,
    REPLACED
}

data class DiffChange(val oldRange: Range, val newRange: Range, val kind: DiffChangeKind)

private fun cutFromStart(oldText: String, newText: String): Int {
    val length = Math.min(oldText.length, newText.length)
    if (length == 0)
        return 0
    var cutFromStart = 0
    while (cutFromStart < length) {
        if (oldText[cutFromStart] != newText[cutFromStart])
            break
        ++cutFromStart
    }
    return cutFromStart
}

private fun cutFromEnd(oldText: String, newText: String, cutFromStart: Int): Int {
    val length = Math.min(oldText.length, newText.length) - cutFromStart
    if (length <= 0)
        return 0
    var cutFromEnd = 0
    var idxOld = oldText.length - 1
    var idxNew = newText.length - 1
    while (cutFromEnd < length) {
        if (oldText[idxOld--] != newText[idxNew--])
            break
        ++cutFromEnd
    }
    return cutFromEnd
}

fun buildChanges(oldText: String, newText: String): List<DiffChange> {
    val cutFromStart = cutFromStart(oldText, newText)
    val cutFromEnd = cutFromEnd(oldText, newText, cutFromStart)
    val trimmedOldLength = oldText.length - cutFromEnd - cutFromStart
    val trimmedNewLength = newText.length - cutFromEnd - cutFromStart

    val changes = mutableListOf<DiffChange>()
    if (cutFromStart > 0) {
        val commonPrefixRange = Range(0, cutFromStart)
        changes.add(DiffChange(commonPrefixRange, commonPrefixRange, DiffChangeKind.EQUAL))
    }

    val threshold = 1024
    if (trimmedOldLength >= threshold && trimmedNewLength >= threshold) {
        val middleOldRange = Range(cutFromStart, oldText.length - cutFromEnd)
        val middleNewRange = Range(cutFromStart, newText.length - cutFromEnd)
        if (!middleOldRange.isEmpty() && middleNewRange.isEmpty()) {
            changes.add(DiffChange(middleOldRange, middleNewRange, DiffChangeKind.DELETED))
        } else if (middleOldRange.isEmpty() && !middleNewRange.isEmpty()) {
            changes.add(DiffChange(middleOldRange, middleNewRange, DiffChangeKind.INSERTED))
        } else if (!middleOldRange.isEmpty() && !middleNewRange.isEmpty()) {
            changes.add(DiffChange(middleOldRange, middleNewRange, DiffChangeKind.REPLACED))
        }
    } else {
        // todo replace naive algorithm by Myers algorithm
        val diffs = computeNaiveLCS(trimmedOldLength, trimmedNewLength, { x1, x2 -> oldText[cutFromStart + x1] == newText[cutFromStart + x2] })

        changes.addAll(diffs.map { DiffChange(it.oldRange.shift(cutFromStart), it.newRange.shift(cutFromStart), it.kind) })
    }

    if (cutFromEnd > 0) {
        val oldCommonSuffixRange = Range(oldText.length - cutFromEnd, oldText.length)
        val newCommonSuffixRange = Range(newText.length - cutFromEnd, newText.length)
        changes.add(DiffChange(oldCommonSuffixRange, newCommonSuffixRange, DiffChangeKind.EQUAL))
    }
    return changes
}


// naive Wagner & Fischer algorithm
fun computeNaiveLCS(length1: Int, length2: Int, comparer: (Int, Int) -> Boolean): List<DiffChange> {
    val d = Array(length2 + 1) { Array(length1 + 1) { 0 } }

    for (idx1 in 0..length1)
        d[0][idx1] = idx1
    for (idx2 in 0..length2)
        d[idx2][0] = idx2

    for (idx2 in 1..length2) {
        for (idx1 in 1..length1) {
            val deleteWeight = d[idx2][idx1 - 1] + 1
            val insertWeight = d[idx2 - 1][idx1] + 1
            val replaceWeight = if (comparer(idx1 - 1, idx2 - 1)) d[idx2 - 1][idx1 - 1] + 0 else 1024 * 1024
            d[idx2][idx1] = Math.min(Math.min(deleteWeight, insertWeight), replaceWeight)
        }
    }

    val path = mutableListOf<DiffChange>()

    var idx1 = length1
    var idx2 = length2

    var lastKind: DiffChangeKind? = null
    var lastIdx1 = idx1 + 1
    var lastIdx2 = idx2 + 1
    while (idx1 > 0 || idx2 > 0) {
        val deleteStepWeight = if (idx1 >= 1) d[idx2][idx1 - 1] else Int.MAX_VALUE
        val insertStepWeight = if (idx2 >= 1) d[idx2 - 1][idx1] else Int.MAX_VALUE
        val replaceStepWeight = if (idx1 >= 1 && idx2 >= 1 && comparer(idx1 - 1, idx2 - 1)) d[idx2 - 1][idx1 - 1] else Int.MAX_VALUE

        val kind: DiffChangeKind =
            if (deleteStepWeight < insertStepWeight && deleteStepWeight < replaceStepWeight) {
                DiffChangeKind.DELETED
            } else if (insertStepWeight < replaceStepWeight) {
                DiffChangeKind.INSERTED
            } else {
                if (comparer(idx1 - 1, idx2 - 1)) DiffChangeKind.EQUAL else DiffChangeKind.REPLACED
            }

        if (lastKind == null) {
            lastKind = kind
        } else if (lastKind != kind) {
            path.add(DiffChange(Range(idx1 + 1 - 1, lastIdx1 - 1), Range(idx2 + 1 - 1, lastIdx2 - 1), lastKind))
            lastKind = kind
            lastIdx1 = idx1 + 1
            lastIdx2 = idx2 + 1
        }

        when (kind) {
            DiffChangeKind.DELETED -> idx1--
            DiffChangeKind.INSERTED -> idx2--
            DiffChangeKind.EQUAL, DiffChangeKind.REPLACED -> {
                idx1--
                idx2--
            }
        }
    }
    if (lastKind != null) {
        path.add(DiffChange(Range(idx1 + 1 - 1, lastIdx1 - 1), Range(idx2 + 1 - 1, lastIdx2 - 1), lastKind))
    }
    return path.reversed().toList()
}



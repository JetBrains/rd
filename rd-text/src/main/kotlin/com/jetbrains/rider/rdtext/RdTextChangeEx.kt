package com.jetbrains.rider.rdtext

import com.jetbrains.rider.rdtext.intrinsics.RdTextChange
import com.jetbrains.rider.rdtext.intrinsics.RdTextChangeKind

fun RdTextChange.delta(): Int {
    if (RdTextChangeKind.Reset == kind) throw UnsupportedOperationException()
    return new.length - old.length
}

fun RdTextChange.reverse(): RdTextChange {
    val newKind = when (kind) {
        RdTextChangeKind.Insert -> RdTextChangeKind.Remove
        RdTextChangeKind.Remove -> RdTextChangeKind.Insert
        RdTextChangeKind.Replace -> RdTextChangeKind.Replace
        RdTextChangeKind.Reset -> throw UnsupportedOperationException()
    }

    return RdTextChange(newKind, startOffset, new, old, fullTextLength - delta())
}

fun RdTextChange.assertDocumentLength(current: Int) {
    if (RdTextChangeKind.Reset == kind) return

    val actualLength = current + delta()
    val expectedLength = fullTextLength
    if (actualLength != expectedLength)
        throw IllegalStateException("Expected document length: $expectedLength, but actual: $actualLength")
}
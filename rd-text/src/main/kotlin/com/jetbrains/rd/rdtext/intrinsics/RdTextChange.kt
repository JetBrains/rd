package com.jetbrains.rd.rdtext.intrinsics

import com.jetbrains.rd.util.string.IPrintable
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.printToString

enum class RdTextChangeKind {
    Insert,
    Remove,
    Replace,
    Reset,
    PromoteVersion,
    InsertLeftSide,
    InsertRightSide
}

class RdTextChange(val kind: RdTextChangeKind,
                   val startOffset: Int,
                   val old: String,
                   val new: String,
                   val fullTextLength: Int) : IPrintable {
    companion object {
        private fun escape(s: String): String = s.replace("\n", "\\n").replace("\r", "\\r")
    }

    override fun print(printer: PrettyPrinter) {
        printer.println("RdTextChange (")
        printer.indent {
            println("kind = $kind")
            println("startOffset = $startOffset")
            println("old = '${escape(old)}'")
            println("new = '${escape(new)}'")
            println("fullTextLength = $fullTextLength")
        }
        printer.println(")")
    }

    override fun toString(): String = this.printToString()
}

val RdTextChange.isNormalChange: Boolean get() = kind != RdTextChangeKind.Reset && kind != RdTextChangeKind.PromoteVersion

fun RdTextChange.delta(): Int {
    if (!isNormalChange) throw UnsupportedOperationException()
    return new.length - old.length
}

fun RdTextChange.reverse(): RdTextChange {
    val newKind = when (kind) {
        RdTextChangeKind.Insert -> RdTextChangeKind.Remove
        RdTextChangeKind.Remove -> RdTextChangeKind.Insert
        RdTextChangeKind.Replace -> RdTextChangeKind.Replace
        RdTextChangeKind.Reset -> throw UnsupportedOperationException()
        RdTextChangeKind.PromoteVersion -> throw UnsupportedOperationException()
        RdTextChangeKind.InsertLeftSide -> RdTextChangeKind.Remove
        RdTextChangeKind.InsertRightSide -> RdTextChangeKind.Remove
    }

    val textLength = if (fullTextLength == -1) -1 else fullTextLength - delta()
    return RdTextChange(newKind, startOffset, new, old, textLength)
}

fun RdTextChange.assertDocumentLength(current: Int) {
    if (!isNormalChange) return

    val actualLength = current + delta()
    val expectedLength = fullTextLength
    if (actualLength != expectedLength)
        throw IllegalStateException("Expected document length: $expectedLength, but actual: $actualLength")
}
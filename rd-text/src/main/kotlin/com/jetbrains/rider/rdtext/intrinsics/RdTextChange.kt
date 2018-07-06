package com.jetbrains.rider.rdtext.intrinsics

import com.jetbrains.rider.util.string.IPrintable
import com.jetbrains.rider.util.string.PrettyPrinter
import com.jetbrains.rider.util.string.printToString

enum class RdTextChangeKind {
    Insert,
    Remove,
    Replace,
    Reset
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
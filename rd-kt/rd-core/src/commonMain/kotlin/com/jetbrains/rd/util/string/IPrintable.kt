package com.jetbrains.rd.util.string

import com.jetbrains.rd.util.getThrowableText

/**
 * An object that can be converted to a structured text representation for debugging purposes.
 */
interface IPrintable {
    fun print(printer: PrettyPrinter) = printer.print(this::class.simpleName ?: this.toString())
}

fun Any?.print(printer: PrettyPrinter) {
    when (this) {
        is Unit -> printer.print("<unit>")
        is IPrintable -> this.print(printer)
        null -> printer.print("<null>")
        is String -> printer.print("\"$this\"")
        is Throwable -> printer.print(getThrowableText())
        is List<*> -> {
            val maxPrint = 3
            printer.print("[")
            val cnt = this@print.size
            printer.indent {
                this@print.take(maxPrint).forEach {
                    println()
                    it.print(printer)
                }
                if (cnt > maxPrint) {
                    println()
                    print("... and ${cnt - maxPrint} more")
                }
                if (cnt > 0) println()
                else print("<empty>")
            }
            printer.print("]")

        }
        is BooleanArray -> {
            this.joinToString().print(printer)
        }
        else -> printer.print(toString())
    }
}

fun Any?.println(printer: PrettyPrinter) {
    print(printer)
    printer.println()
}

fun Any?.printToString() = PrettyPrinter().apply { this@printToString.print(this@apply) }.toString()

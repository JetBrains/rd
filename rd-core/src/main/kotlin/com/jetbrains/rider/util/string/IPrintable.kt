package com.jetbrains.rider.util.string

/**
 * An object that can be converted to a structured text representation for debugging purposes.
 */
interface IPrintable {
    fun print(printer: PrettyPrinter) = printer.print(javaClass.simpleName)
}

fun Any?.print(printer: PrettyPrinter) {
    when (this) {
        is IPrintable -> this.print(printer)
        null -> printer.print("<null>")
        is String -> printer.print("\"$this\"")
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
        else -> printer.print(toString())
    }
}

fun Any?.printToString() = PrettyPrinter().apply { this@printToString.print(this@apply) }.toString()

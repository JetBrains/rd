package com.jetbrains.rd.util.threading

import java.io.PrintStream
import java.io.PrintWriter

class CompoundThrowable(private val errors: List<Throwable>) : Throwable() {

    /*override */override val message: String get() {
        val acc = StringBuilder()
        combineErrors({ acc.appendln(it.message) }, { acc.appendln(it) })
        return acc.toString()
    }

    /*override */fun getLocalizedMessage(): String {
        val acc = StringBuilder()
        combineErrors({
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            acc.appendln((it as java.lang.Throwable).localizedMessage)
        }, { acc.appendln(it) })
        return acc.toString()
    }

    override fun toString(): String {
        val acc = StringBuilder()
        combineErrors({ acc.appendln(it.toString()) }, { acc.appendln(it) })
        return acc.toString()
    }

    /*override */fun printStackTrace(s: PrintStream) {
        combineErrors({ it.printStackTrace(s); s.println() }, { s.println(it) })
    }

    /*override */fun printStackTrace(s: PrintWriter) {
        combineErrors({ it.printStackTrace(s); s.println() }, { s.println(it) })
    }

    private inline fun combineErrors(printEx: (Throwable) -> Unit, printLn: (String) -> Unit) {
        if (errors.size == 1) {
            printEx(errors.single())
            return
        }

        printLn("CompositeException (${errors.size} nested):")
        printLn("-------------------------------------------")

        errors.withIndex().forEach { p ->
            printLn("[${p.index}]")
            printEx(p.value)
        }

        printLn("-------------------------------------------")
    }

    companion object {
        fun throwIfNotEmpty(errors: List<Throwable>) = when(errors.size) {
            0 -> Unit
            1 -> throw errors.single()
            else -> throw CompoundThrowable(errors)
        }
    }
}

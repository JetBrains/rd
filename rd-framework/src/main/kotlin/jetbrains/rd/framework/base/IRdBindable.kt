package com.jetbrains.rider.framework.base

import com.jetbrains.rider.framework.IIdentities
import com.jetbrains.rider.framework.IRdDynamic
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.string.PrettyPrinter

interface IRdBindable : IRdDynamic, IPrintable {
    fun bind(lf: Lifetime, parent: IRdDynamic, name: String)
    fun identify(ids: IIdentities)
}

interface IPrintable {
    fun print(printer: PrettyPrinter) = printer.print(this::class.simpleName ?: "IPrintable")
}


//generator comprehension methods
fun <T:IRdBindable?> T.bind(lf: Lifetime, parent: IRdDynamic, name: String) = this?.bind(lf, parent, name)
fun <T:IRdBindable?> T.identify(ids: IIdentities) = this?.identify(ids)

fun <T:IRdBindable?> Array<T>.identify(ids: IIdentities) = forEach { v ->  v?.identify(ids)}
fun <T:IRdBindable?> Array<T>.bind(lf: Lifetime, parent: IRdDynamic, name: String) = forEachIndexed { i, v ->  v?.bind(lf,parent, "$name[$i]")}

fun <T:IRdBindable?> List<T>.identify(ids: IIdentities) = forEach { v ->  v?.identify(ids)}
fun <T:IRdBindable?> List<T>.bind(lf: Lifetime, parent: IRdDynamic, name: String) = forEachIndexed { i, v ->  v?.bind(lf,parent, "$name[$i]")}

internal fun Any.identifyPolymorphic(ids: IIdentities) {
    (this as? IRdBindable).identify(ids)
    (this as? Array<*>)?.forEach { v ->  (v as? IRdBindable)?.identify(ids)}
    (this as? List<*>)?.forEach { v ->  (v as? IRdBindable)?.identify(ids)}
}

internal fun Any.bindPolymorphic(lf: Lifetime, parent: IRdDynamic, name: String) {
    (this as? IRdBindable).bind(lf, parent, name)
    (this as? Array<*>)?.forEachIndexed { i, v ->  (v as? IRdBindable)?.bind(lf,parent, "$name[$i]")}
    (this as? List<*>)?.forEachIndexed { i, v ->  (v as? IRdBindable)?.bind(lf,parent, "$name[$i]")}
}

internal fun Any.bindPolymorphic(ids: IIdentities) {

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


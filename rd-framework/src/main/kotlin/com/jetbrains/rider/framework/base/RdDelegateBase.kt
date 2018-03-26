package com.jetbrains.rider.framework.base

import com.jetbrains.rider.util.string.IPrintable
import com.jetbrains.rider.util.string.PrettyPrinter

abstract class RdDelegateBase<out T : RdBindableBase>(val delegatedBy: T) : IRdBindable by delegatedBy, IPrintable {
    override fun print(printer: PrettyPrinter) {
        printer.print("${this::class.simpleName} delegated by ")
        delegatedBy.print(printer)
    }
}
package com.jetbrains.rd.framework.base

import com.jetbrains.rd.util.string.IPrintable
import com.jetbrains.rd.util.string.PrettyPrinter

abstract class RdDelegateBase<out T : RdBindableBase>(val delegatedBy: T) : IRdBindable by delegatedBy, IPrintable {
    override fun print(printer: PrettyPrinter) {
        printer.print("${this::class.simpleName} delegated by ")
        delegatedBy.print(printer)
    }

    abstract override fun deepClone(): IRdBindable
}
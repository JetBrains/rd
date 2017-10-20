package com.jetbrains.rider.framework.base

import com.jetbrains.rider.framework.IdKind
import com.jetbrains.rider.framework.RdId
import com.jetbrains.rider.util.string.PrettyPrinter

interface IRdReactive : IRdBindable {
    var id: RdId
    var async: Boolean

    override fun print(printer: PrettyPrinter) {
        super.print(printer)
        printer.print("(id:$id)")
    }
}

internal fun <T : IRdReactive> T.withId(id: RdId) : T {
    require(this.id == RdId.Null) {"this.id != RdId.Null, but ${this.id}"}
    require(id != RdId.Null) {"id != RdId.Null"}

    return this.apply { this.id = id }
}

fun <T: IRdReactive> T.static(id: Int) : T = withId(RdId(IdKind.StaticEntity, id))


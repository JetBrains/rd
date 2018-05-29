package com.jetbrains.rider.rdtext.intrinsics

import com.jetbrains.rider.framework.IIdentities
import com.jetbrains.rider.framework.IMarshaller
import com.jetbrains.rider.framework.RdId
import com.jetbrains.rider.framework.SerializationCtx
import com.jetbrains.rider.framework.AbstractBuffer
import com.jetbrains.rider.framework.base.RdBindableBase
import com.jetbrains.rider.framework.impl.RdSignal
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.rdtext.impl.ot.OtOperation
import com.jetbrains.rider.util.reactive.ISignal
import com.jetbrains.rider.util.string.PrettyPrinter
import kotlin.reflect.*

// auto-generated class
class RdOtState (
        private val _operation : RdSignal<OtOperation>,
        private val _ack : RdSignal<OtOperation>
) : RdBindableBase() {
    //companion

    companion object : IMarshaller<RdOtState> {
        override val _type: KClass<RdOtState> = RdOtState::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdOtState {
            val _operation = RdSignal.read(ctx, buffer, OtOperationMarshaller)
            val _ack = RdSignal.read(ctx, buffer, OtOperationMarshaller)
            return RdOtState(_operation, _ack)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdOtState) {
            RdSignal.write(ctx, buffer, value._operation)
            RdSignal.write(ctx, buffer, value._ack)
        }

    }
    //fields
    val operation : ISignal<OtOperation> get() = _operation
    val ack : ISignal<OtOperation> get() = _ack

    //initializer
    //secondary constructor
    constructor(
    ) : this (
            RdSignal<OtOperation>(OtOperationMarshaller),
            RdSignal<OtOperation>(OtOperationMarshaller)
    )

    //init method
    override fun init(lifetime: Lifetime) {
        _operation.bind(lifetime, this, "operation")
        _ack.bind(lifetime, this, "ack")
    }
    //identify method
    override fun identify(identities: IIdentities, id: RdId) {
        _operation.identify(identities, id.mix(".operation"))
        _ack.identify(identities, id.mix(".ack"))
    }
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("RdOtState (")
        printer.indent {
            print("operation = "); _operation.print(printer); println()
            print("ack = "); _ack.print(printer); println()
        }
        printer.print(")")
    }
}
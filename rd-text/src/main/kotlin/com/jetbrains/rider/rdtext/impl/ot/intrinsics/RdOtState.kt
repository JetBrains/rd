package com.jetbrains.rider.rdtext.impl.ot.intrinsics

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.RdBindableBase
import com.jetbrains.rider.framework.base.withId
import com.jetbrains.rider.framework.impl.RdOptionalProperty
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.framework.impl.RdSignal
import com.jetbrains.rider.util.reactive.IOptProperty
import com.jetbrains.rider.util.reactive.IProperty
import com.jetbrains.rider.util.reactive.ISignal
import com.jetbrains.rider.util.string.PrettyPrinter
import kotlin.reflect.*

// auto-generated class
class RdOtState private constructor(
        private val _operation : RdProperty<com.jetbrains.rider.rdtext.impl.ot.OtOperation?>,
        private val _ack : RdSignal<com.jetbrains.rider.rdtext.impl.ot.RdAck>,
        private val _assertedMasterText : RdOptionalProperty<com.jetbrains.rider.rdtext.impl.intrinsics.RdAssertion>,
        private val _assertedSlaveText : RdOptionalProperty<com.jetbrains.rider.rdtext.impl.intrinsics.RdAssertion>
) : RdBindableBase() {
    //companion

    companion object : IMarshaller<RdOtState> {
        override val _type: KClass<RdOtState> = RdOtState::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdOtState {
            val _id = RdId.read(buffer)
            val _operation = RdProperty.read(ctx, buffer, __OtOperationNullableSerializer)
            val _ack = RdSignal.read(ctx, buffer, com.jetbrains.rider.rdtext.impl.ot.intrinsics.RdAckMarshaller)
            val _assertedMasterText = RdOptionalProperty.read(ctx, buffer, com.jetbrains.rider.rdtext.impl.intrinsics.RdAssertion)
            val _assertedSlaveText = RdOptionalProperty.read(ctx, buffer, com.jetbrains.rider.rdtext.impl.intrinsics.RdAssertion)
            return RdOtState(_operation, _ack, _assertedMasterText, _assertedSlaveText).withId(_id)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdOtState) {
            value.rdid.write(buffer)
            RdProperty.write(ctx, buffer, value._operation)
            RdSignal.write(ctx, buffer, value._ack)
            RdOptionalProperty.write(ctx, buffer, value._assertedMasterText)
            RdOptionalProperty.write(ctx, buffer, value._assertedSlaveText)
        }

        private val __OtOperationNullableSerializer = com.jetbrains.rider.rdtext.impl.ot.intrinsics.OtOperationMarshaller.nullable()
    }
    //fields
    val operation : IProperty<com.jetbrains.rider.rdtext.impl.ot.OtOperation?> get() = _operation
    val ack : ISignal<com.jetbrains.rider.rdtext.impl.ot.RdAck> get() = _ack
    val assertedMasterText : IOptProperty<com.jetbrains.rider.rdtext.impl.intrinsics.RdAssertion> get() = _assertedMasterText
    val assertedSlaveText : IOptProperty<com.jetbrains.rider.rdtext.impl.intrinsics.RdAssertion> get() = _assertedSlaveText

    //initializer
    init {
        _operation.optimizeNested = true
        _assertedMasterText.optimizeNested = true
        _assertedSlaveText.optimizeNested = true
    }

    init {
        bindableChildren.add("operation" to _operation)
        bindableChildren.add("ack" to _ack)
        bindableChildren.add("assertedMasterText" to _assertedMasterText)
        bindableChildren.add("assertedSlaveText" to _assertedSlaveText)
    }

    //secondary constructor
    constructor(
    ) : this (
            RdProperty<com.jetbrains.rider.rdtext.impl.ot.OtOperation?>(null, __OtOperationNullableSerializer),
            RdSignal<com.jetbrains.rider.rdtext.impl.ot.RdAck>(com.jetbrains.rider.rdtext.impl.ot.intrinsics.RdAckMarshaller),
            RdOptionalProperty<com.jetbrains.rider.rdtext.impl.intrinsics.RdAssertion>(com.jetbrains.rider.rdtext.impl.intrinsics.RdAssertion),
            RdOptionalProperty<com.jetbrains.rider.rdtext.impl.intrinsics.RdAssertion>(com.jetbrains.rider.rdtext.impl.intrinsics.RdAssertion)
    )

    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("RdOtState (")
        printer.indent {
            print("operation = "); _operation.print(printer); println()
            print("ack = "); _ack.print(printer); println()
            print("assertedMasterText = "); _assertedMasterText.print(printer); println()
            print("assertedSlaveText = "); _assertedSlaveText.print(printer); println()
        }
        printer.print(")")
    }
}
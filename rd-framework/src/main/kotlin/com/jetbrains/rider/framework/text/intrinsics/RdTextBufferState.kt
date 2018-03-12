package com.jetbrains.rider.framework.text.intrinsics

import com.jetbrains.rider.framework.IMarshaller
import com.jetbrains.rider.framework.RdId
import com.jetbrains.rider.framework.SerializationCtx
import com.jetbrains.rider.framework.base.*
import com.jetbrains.rider.framework.impl.RdOptionalProperty
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.framework.nullable
import com.jetbrains.rider.util.reactive.IOptProperty
import com.jetbrains.rider.util.reactive.IProperty
import com.jetbrains.rider.util.string.PrettyPrinter

class RdTextBufferState (
        private val _changes : RdProperty<RdTextBufferChange?>,
        private val _assertedMasterText : RdOptionalProperty<RdAssertion>,
        private val _assertedSlaveText : RdOptionalProperty<RdAssertion>
) : RdBindableBase() {
    //companion

    companion object : IMarshaller<RdTextBufferState> {
        override val _type: Class<RdTextBufferState> = RdTextBufferState::class.java

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdTextBufferState {
            val _id = RdId.read(buffer)
            val _changes = RdProperty.read(ctx, buffer, __RdTextBufferChangeNullableSerializer)
            val _assertedMasterText = RdOptionalProperty.read(ctx, buffer, RdAssertion)
            val _assertedSlaveText = RdOptionalProperty.read(ctx, buffer, RdAssertion)
            return RdTextBufferState(_changes, _assertedMasterText, _assertedSlaveText).withId(_id)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdTextBufferState) {
            value.rdid.write(buffer)
            RdProperty.write(ctx, buffer, value._changes)
            RdOptionalProperty.write(ctx, buffer, value._assertedMasterText)
            RdOptionalProperty.write(ctx, buffer, value._assertedSlaveText)
        }

        private val __RdTextBufferChangeNullableSerializer = com.jetbrains.rider.framework.text.intrinsics.RdTextBufferChange.nullable()
    }
    //fields
    val changes : IProperty<RdTextBufferChange?> get() = _changes
    val assertedMasterText : IOptProperty<RdAssertion> get() = _assertedMasterText
    val assertedSlaveText : IOptProperty<RdAssertion> get() = _assertedSlaveText

    //initializer
    init {
        _changes.optimizeNested = true
        _assertedMasterText.optimizeNested = true
        _assertedSlaveText.optimizeNested = true
    }

    init {
        bindableChildren.add("changes" to _changes)
        bindableChildren.add("assertedMasterText" to _assertedMasterText)
        bindableChildren.add("assertedSlaveText" to _assertedSlaveText)
    }

    //secondary constructor
    constructor(
    ) : this (
            RdProperty<RdTextBufferChange?>(null, __RdTextBufferChangeNullableSerializer),
            RdOptionalProperty<RdAssertion>(RdAssertion),
            RdOptionalProperty<RdAssertion>(RdAssertion)
    )

    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter) {
        printer.println("RdTextBufferState (")
        printer.indent {
            print("changes = "); _changes.print(printer); println()
            print("assertedMasterText = "); _assertedMasterText.print(printer); println()
            print("assertedSlaveText = "); _assertedSlaveText.print(printer); println()
        }
        printer.print(")")
    }
}
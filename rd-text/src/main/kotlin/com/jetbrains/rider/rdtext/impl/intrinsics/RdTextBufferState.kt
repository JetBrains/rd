package com.jetbrains.rider.rdtext.impl.intrinsics

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.*
import com.jetbrains.rider.framework.impl.RdOptionalProperty
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.util.reactive.IOptProperty
import com.jetbrains.rider.util.reactive.IProperty
import com.jetbrains.rider.util.string.PrettyPrinter
import kotlin.reflect.*

class RdTextBufferState private constructor(
        private val _changes: RdProperty<RdTextBufferChange?>,
        private val _versionBeforeTypingSession: RdOptionalProperty<com.jetbrains.rider.rdtext.intrinsics.TextBufferVersion>,
        private val _assertedMasterText: RdOptionalProperty<RdAssertion>,
        private val _assertedSlaveText: RdOptionalProperty<RdAssertion>
) : RdBindableBase() {
    //companion

    companion object : IMarshaller<RdTextBufferState> {
        override val _type: KClass<RdTextBufferState> = RdTextBufferState::class

        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RdTextBufferState {
            val _id = RdId.read(buffer)
            val _changes = RdProperty.read(ctx, buffer, __RdTextBufferChangeNullableSerializer)
            val _versionBeforeTypingSession = RdOptionalProperty.read(ctx, buffer, com.jetbrains.rider.rdtext.intrinsics.TextBufferVersionMarshaller)
            val _assertedMasterText = RdOptionalProperty.read(ctx, buffer, RdAssertion)
            val _assertedSlaveText = RdOptionalProperty.read(ctx, buffer, RdAssertion)
            return RdTextBufferState(_changes, _versionBeforeTypingSession, _assertedMasterText, _assertedSlaveText).withId(_id)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RdTextBufferState) {
            value.rdid.write(buffer)
            RdProperty.write(ctx, buffer, value._changes)
            RdOptionalProperty.write(ctx, buffer, value._versionBeforeTypingSession)
            RdOptionalProperty.write(ctx, buffer, value._assertedMasterText)
            RdOptionalProperty.write(ctx, buffer, value._assertedSlaveText)
        }

        private val __RdTextBufferChangeNullableSerializer = com.jetbrains.rider.rdtext.impl.intrinsics.RdTextBufferChange.nullable()
    }
    //fields
    val changes: IProperty<RdTextBufferChange?> get() = _changes
    val versionBeforeTypingSession: IOptProperty<com.jetbrains.rider.rdtext.intrinsics.TextBufferVersion> get() = _versionBeforeTypingSession
    val assertedMasterText: IOptProperty<RdAssertion> get() = _assertedMasterText
    val assertedSlaveText: IOptProperty<RdAssertion> get() = _assertedSlaveText
    //initializer
    init {
        _changes.optimizeNested = true
        _versionBeforeTypingSession.optimizeNested = true
        _assertedMasterText.optimizeNested = true
        _assertedSlaveText.optimizeNested = true
    }

    init {
        bindableChildren.add("changes" to _changes)
        bindableChildren.add("versionBeforeTypingSession" to _versionBeforeTypingSession)
        bindableChildren.add("assertedMasterText" to _assertedMasterText)
        bindableChildren.add("assertedSlaveText" to _assertedSlaveText)
    }

    //secondary constructor
    constructor(
    ) : this(
            RdProperty<RdTextBufferChange?>(null, __RdTextBufferChangeNullableSerializer),
            RdOptionalProperty<com.jetbrains.rider.rdtext.intrinsics.TextBufferVersion>(com.jetbrains.rider.rdtext.intrinsics.TextBufferVersionMarshaller),
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
            print("versionBeforeTypingSession = "); _versionBeforeTypingSession.print(printer); println()
            print("assertedMasterText = "); _assertedMasterText.print(printer); println()
            print("assertedSlaveText = "); _assertedSlaveText.print(printer); println()
        }
        printer.print(")")
    }
}
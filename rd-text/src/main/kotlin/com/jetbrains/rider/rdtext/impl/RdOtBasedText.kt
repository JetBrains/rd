package com.jetbrains.rider.rdtext.impl

import com.jetbrains.rider.framework.IRdDynamic
import com.jetbrains.rider.framework.base.RdDelegateBase
import com.jetbrains.rider.rdtext.intrinsics.RdOtState
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.ot.OtRole
import com.jetbrains.rider.util.ot.OtState
import com.jetbrains.rider.util.ot.TextChange
import com.jetbrains.rider.util.reactive.ISignal
import com.jetbrains.rider.util.reactive.flowInto

@Suppress("unused")
class RdOtBasedText(delegate: RdOtState) : RdDelegateBase<RdOtState>(delegate), ISignal<TextChange> {
    private val otState = OtState(OtRole.Master)
    private val protocolChanges = delegatedBy.operation
    private val protocolAck = delegatedBy.ack

    override fun bind(lf: Lifetime, parent: IRdDynamic, name: String) {
        super.bind(lf, parent, name)

        protocolChanges.flowInto(lf, otState.receiveOperation)
        otState.sendOperation.flowInto(lf, protocolChanges)

        protocolAck.flowInto(lf, otState.receiveAck)
        otState.sendAck.flowInto(lf, protocolAck)
    }

    override fun advise(lifetime: Lifetime, handler: (TextChange) -> Unit) = otState.advise(lifetime, handler)

    override fun fire(value: TextChange) = otState.fire(value)
}
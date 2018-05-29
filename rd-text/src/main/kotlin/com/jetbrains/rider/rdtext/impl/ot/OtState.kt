package com.jetbrains.rider.rdtext.impl.ot

import com.jetbrains.rider.util.getLogger
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.*

open class OtState(private val role: OtRole) : ISignal<TextChange> {
    private val logger = getLogger<OtState>()

    protected var diff: OtOperation = OtOperation(emptyList(), role)

    private val mySendOperation = Signal<OtOperation>()
    private val myReceiveOperation = Signal<OtOperation>()
    private val mySendAck = Signal<OtOperation>()
    private val myReceiveAck = Signal<OtOperation>()

    val sendOperation: ISource<OtOperation> get() = mySendOperation
    val receiveOperation: ISignal<OtOperation> get() = myReceiveOperation
    val sendAck: ISource<OtOperation> get() = mySendAck
    val receiveAck: ISignal<OtOperation> get() = myReceiveAck

    private val textChanged = Signal<TextChange>()

    init {
        myReceiveOperation.adviseEternal { operation ->
            val (newLocalDiff, localizedOpToApply) = transform(diff, operation)
            diff = newLocalDiff

            applyOperation(localizedOpToApply)

            mySendAck.fire(localizedOpToApply.invert())
        }

        myReceiveAck.adviseEternal { ack ->
            diff = compose(diff, ack)
        }
    }

    override fun advise(lifetime: Lifetime, handler: (TextChange) -> Unit) = textChanged.advise(lifetime, handler)

    override fun fire(value: TextChange) {
        val op = value.toOperation(role)
        sendOperation(op)
    }

    protected open fun sendOperation(op: OtOperation) {
        mySendOperation.fire(op)
        diff = compose(diff, op)
    }

    protected open fun applyOperation(op: OtOperation) {
        op.toTextChanges().forEach { textChanged.fire(it) }
    }

    override fun toString(): String {
        return "OtStat(role=$role, diff=$diff)"
    }
}


enum class OtRole {
    Slave,
    Master
}
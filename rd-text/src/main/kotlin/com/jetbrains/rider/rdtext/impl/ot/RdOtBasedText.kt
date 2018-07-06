package com.jetbrains.rider.rdtext.impl.ot

import com.jetbrains.rider.framework.IRdDynamic
import com.jetbrains.rider.framework.base.RdDelegateBase
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.rdtext.*
import com.jetbrains.rider.rdtext.impl.intrinsics.RdAssertion
import com.jetbrains.rider.rdtext.impl.intrinsics.RdChangeOrigin
import com.jetbrains.rider.rdtext.impl.ot.intrinsics.RdAck
import com.jetbrains.rider.rdtext.impl.ot.intrinsics.RdOtState
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.*
import com.jetbrains.rider.util.reflection.usingTrueFlag


open class RdOtBasedText(delegate: RdOtState, final override val isMaster: Boolean = true) : RdDelegateBase<RdOtState>(delegate), ITextBuffer {
    private var isComplexChange = false
    private val textChanged: IOptProperty<RdTextChange> = OptProperty()

    private var diff: MutableList<OtOperation> = mutableListOf()

    private val _historyChanged: ISignal<RdTextChange> = Signal()

    override val historyChanged: ISource<RdTextChange> get() = _historyChanged
    final override var bufferVersion: TextBufferVersion = TextBufferVersion.INIT_VERSION
        private set

    private val localOrigin: RdChangeOrigin = if (isMaster) RdChangeOrigin.Master else RdChangeOrigin.Slave

    init {
        (delegatedBy.operation as RdProperty<*>).slave()
    }

    override fun bind(lf: Lifetime, parent: IRdDynamic, name: String) {
        super.bind(lf, parent, name)

        delegatedBy.operation.adviseNotNull(lf, {
            if (it.origin != localOrigin) receiveOperation(it)
        })
        delegatedBy.ack.advise(lf) {
            if (it.origin != localOrigin) updateHistory(it)
        }

        // for asserting documents equality
        delegatedBy.assertedSlaveText.compose(delegatedBy.assertedMasterText, { slave, master -> slave to master}).advise(lf) { (s, m) ->
            if (s.masterVersion == m.masterVersion
                    && s.slaveVersion == m.slaveVersion
                    && s.text != m.text) {
                throw IllegalStateException("Master and Slave texts are different.\nMaster:\n$m\nSlave:\n$s")
            }
        }
    }

    private fun updateHistory(ack: RdAck) {
        val ts = ack.timestamp
        diff.removeIf { it.timestamp == ts }
    }

    protected open fun receiveOperation(operation: OtOperation) {
        val remoteOrigin = operation.origin
        val transformedOp = when (operation.kind) {
            OtOperationKind.Normal -> {
                val (op, newDiff) = diff.fold(operation to mutableListOf<OtOperation>(), { (transformedOp, transformedDiff), localChange ->
                    val (transformedLocalChange, newTransformedOp) = transform(localChange, transformedOp)
                    transformedDiff.add(transformedLocalChange)
                    return@fold newTransformedOp to transformedDiff
                })

                diff = newDiff
                op
            }
            OtOperationKind.Reset -> {
                diff = mutableListOf()
                operation
            }
        }

        bufferVersion = if (remoteOrigin == RdChangeOrigin.Master)
            bufferVersion.incrementMaster()
        else
            bufferVersion.incrementSlave()

        val timestamp = operation.timestamp
        assert(if (remoteOrigin == RdChangeOrigin.Master) bufferVersion.master == timestamp else bufferVersion.slave == timestamp)

        val changes = transformedOp.toRdTextChanges()

        for ((i, ch) in changes.withIndex()) {
            usingTrueFlag(RdOtBasedText::isComplexChange, i < changes.lastIndex) {
                _historyChanged.fire(ch)
                textChanged.set(ch)
            }
        }


        if (operation.kind == OtOperationKind.Normal)
            sendAck(timestamp)
    }

    protected open fun sendAck(timestamp: Int) {
        delegatedBy.ack.fire(RdAck(timestamp, localOrigin))
    }

    override fun advise(lifetime: Lifetime, handler: (RdTextChange) -> Unit) {
        require(delegatedBy.isBound)
        protocol.scheduler.assertThread()

        textChanged.advise(lifetime, handler)
    }

    override fun fire(value: RdTextChange) {
        require(delegatedBy.isBound || bufferVersion == TextBufferVersion.INIT_VERSION)
        if (delegatedBy.isBound) protocol.scheduler.assertThread()

        bufferVersion = if (isMaster) bufferVersion.incrementMaster()
        else bufferVersion.incrementSlave()

        val ts = getCurrentTs()
        val operation = value.toOperation(localOrigin, ts)

        when (operation.kind) {
            OtOperationKind.Normal -> diff.add(operation)
            OtOperationKind.Reset -> diff = mutableListOf()
        }

        _historyChanged.fire(value)
        sendOperation(operation)
    }

    protected fun getCurrentTs() = if (isMaster) bufferVersion.master else bufferVersion.slave

    protected open fun sendOperation(operation: OtOperation) {
        delegatedBy.operation.set(operation)
    }

    override fun reset(text: String) {
        fire(RdTextChange(RdTextChangeKind.Reset, 0, "", text, text.length))
    }

    override fun assertState(allText: String) {
        if (isComplexChange) return
        val assertion = RdAssertion(bufferVersion.master, bufferVersion.slave, allText)
        val assertedTextProp = if (isMaster) delegatedBy.assertedMasterText else delegatedBy.assertedSlaveText
        assertedTextProp.set(assertion)
    }

    fun playCaret(caretOffset: Int): Int {
        if (isMaster) return caretOffset

        var newCaretOffset = caretOffset
        for (op in diff) {
            var offset = 0
            for (change in op.changes) {
                when (change) {
                    is DeleteText ->
                        if (offset < newCaretOffset) {
                            newCaretOffset -= change.text.length
                        }
                    is InsertText ->
                        if (offset < newCaretOffset) {
                            newCaretOffset += change.text.length
                        } else if (offset == newCaretOffset && change.priority != OtChangePriority.AfterAllChanges) {
                            newCaretOffset += change.text.length
                        }
                    is Retain -> offset += change.offset
                }
            }
        }
        return newCaretOffset
    }
}
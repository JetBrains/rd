package com.jetbrains.rider.framework.text.impl

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.RdDelegateBase
import com.jetbrains.rider.framework.base.RdReactiveBase.Companion.logReceived
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.framework.text.intrinsics.RdAssertion
import com.jetbrains.rider.framework.text.intrinsics.RdChangeOrigin
import com.jetbrains.rider.framework.text.intrinsics.RdTextBufferChange
import com.jetbrains.rider.framework.text.intrinsics.RdTextBufferState
import com.jetbrains.rider.framework.text.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.*
import com.jetbrains.rider.util.warn


/**
 * Slave of the text buffer supports a list of changes that were introduced locally and can be rolled back when master buffer reports
 * incompatible change
 */
open class RdTextBuffer(delegate: RdTextBufferState, final override val isMaster: Boolean = true) : RdDelegateBase<RdTextBufferState>(delegate), ITextBuffer {
    private val changesToConfirmOrRollback: MutableList<RdTextChange> = arrayListOf()
    private val textChanged: IOptProperty<RdTextChange> = OptProperty()
    private val _historyChanged: ISignal<RdTextChange> = Signal()
    override val historyChanged: ISource<RdTextChange> get() = _historyChanged

    final override var bufferVersion: TextBufferVersion = TextBufferVersion.INIT_VERSION
        private set

    private val localOrigin: RdChangeOrigin = if (isMaster) RdChangeOrigin.Master else RdChangeOrigin.Slave

    init {
        // disabling mastering, text buffer must resolve conflicts by itself
        (delegatedBy.changes as RdProperty<*>).slave()
    }
    override fun bind(lf: Lifetime, parent: IRdDynamic, name: String) {
        super.bind(lf, parent, name)
        delegatedBy.changes.adviseNotNull(lf) {
            if (it.origin != localOrigin) receiveChange(it)
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

    protected open fun receiveChange(textBufferChange: RdTextBufferChange) {
        val (newVersion, remoteOrigin, change) = textBufferChange
        require(remoteOrigin != localOrigin)

        if (change.kind == RdTextChangeKind.Reset) {
            changesToConfirmOrRollback.clear()
        } else {
            if (isMaster) {
                if (newVersion.master != bufferVersion.master) {
                    logReceived.warn { "Rejecting the change '$change'" }
                    // reject the change. we've already sent overriding change
                    return
                }
            } else {
                if (newVersion.slave != bufferVersion.slave) {
                    // rollback the changes and notify external subscribers
                    for (ch in changesToConfirmOrRollback.reversed()) {
                        val reversedChange = ch.reverse()
                        _historyChanged.fire(reversedChange)
                        textChanged.set(reversedChange)
                    }
                } else {
                    // confirm the changes queue
                    changesToConfirmOrRollback.clear()
                }
            }
        }

        // apply
        bufferVersion = newVersion
        _historyChanged.fire(change)
        textChanged.set(change)
    }

    private fun incrementBufferVersion() {
        bufferVersion = if (isMaster) bufferVersion.incrementMaster() else bufferVersion.incrementSlave()
    }

    override fun fire(value: RdTextChange) {
        require(delegatedBy.isBound || bufferVersion == TextBufferVersion.INIT_VERSION)
        if (delegatedBy.isBound) protocol.scheduler.assertThread()

        incrementBufferVersion()
        if (value.kind == RdTextChangeKind.Reset) {
            changesToConfirmOrRollback.clear()
        } else if (!isMaster) {
            changesToConfirmOrRollback.add(value)
        }

        _historyChanged.fire(value)
        sendChange(RdTextBufferChange(bufferVersion, localOrigin, value))
    }

    protected open fun sendChange(value: RdTextBufferChange) {
        delegatedBy.changes.set(value)
    }

    override fun reset(text: String) {
        fire(RdTextChange(RdTextChangeKind.Reset, 0, "", text, text.length))
    }

    override fun advise(lifetime: Lifetime, handler: (RdTextChange) -> Unit) {
        require(delegatedBy.isBound)
        protocol.scheduler.assertThread()

        textChanged.advise(lifetime, handler)
    }

    override fun assertState(allText: String) {
        val assertion = RdAssertion(bufferVersion.master, bufferVersion.slave, allText)
        val assertedTextProp = if (isMaster) delegatedBy.assertedMasterText else delegatedBy.assertedSlaveText
        assertedTextProp.set(assertion)
    }

    override fun toString(): String {
        return "Text Buffer: ($rdid)"
    }
}
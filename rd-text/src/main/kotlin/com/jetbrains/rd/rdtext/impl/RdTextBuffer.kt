package com.jetbrains.rd.rdtext.impl

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.RdDelegateBase
import com.jetbrains.rd.framework.base.RdReactiveBase.Companion.logReceived
import com.jetbrains.rd.framework.impl.RdProperty
import com.jetbrains.rd.rdtext.*
import com.jetbrains.rd.rdtext.impl.intrinsics.*
import com.jetbrains.rd.rdtext.intrinsics.RdTextChange
import com.jetbrains.rd.rdtext.intrinsics.RdTextChangeKind
import com.jetbrains.rd.rdtext.intrinsics.TextBufferVersion
import com.jetbrains.rd.rdtext.intrinsics.reverse
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.warn


/**
 * Slave of the text buffer supports a list of changes that were introduced locally and can be rolled back when master buffer reports
 * incompatible change
 */
open class RdTextBuffer(delegate: RdTextBufferState, final override val isMaster: Boolean = true) : RdDelegateBase<RdTextBufferState>(delegate), ITextBufferWithTypingSession {
    private val changesToConfirmOrRollback: MutableList<RdTextBufferChange> = mutableListOf()
    private val textChanged: IOptProperty<RdTextChange> = OptProperty()
    private val _historyChanged: ISignal<RdTextChange> = Signal()
    override val historyChanged: ISource<RdTextChange> get() = _historyChanged
    private val _discardedBufferVersion: ISignal<TextBufferVersion> = Signal()
    override val discardedBufferVersion: ISource<TextBufferVersion> get() = _discardedBufferVersion
    private var activeTypingSession: TextBufferTypingSession? = null

    final override var bufferVersion: TextBufferVersion = TextBufferVersion.INIT_VERSION
        private set

    private val localOrigin: RdChangeOrigin = if (isMaster) RdChangeOrigin.Master else RdChangeOrigin.Slave

    init {
        // disabling mastering, text buffer must resolve conflicts by itself
        (delegatedBy.changes as RdProperty<*>).slave()
    }

    override val changing: Boolean get() = delegatedBy.changes.changing

    override fun bind(lf: Lifetime, parent: IRdDynamic, name: String) {
        super.bind(lf, parent, name)
        delegatedBy.changes.adviseNotNull(lf) {
            if (it.origin == localOrigin) return@adviseNotNull

            receiveChange(it)
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
        val newVersion = textBufferChange.version
        val remoteOrigin = textBufferChange.origin
        val change = textBufferChange.change
        require(remoteOrigin != localOrigin)

        val activeSession = activeTypingSession
        if (activeSession != null && activeSession.tryPushRemoteChange(textBufferChange)) {
            return
        }

        if (change.kind == RdTextChangeKind.Reset) {
            changesToConfirmOrRollback.clear()
        } else if (change.kind == RdTextChangeKind.PromoteVersion) {
            require(!isMaster) { "!IsMaster" }
            changesToConfirmOrRollback.clear()
            bufferVersion = newVersion
            _historyChanged.fire(change)
            return
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
                        if (ch.version.slave <= newVersion.slave)
                            break
                        val reversedChange = ch.change.reverse()
                        _historyChanged.fire(reversedChange)
                        textChanged.set(reversedChange)
                    }
                    changesToConfirmOrRollback.clear()
                } else {
                    // confirm the changes queue
                    changesToConfirmOrRollback.clear()
                }
            }
        }

        // apply
        bufferVersion = newVersion
        _historyChanged.fire(change)
        if (activeSession == null || !activeSession.isCommitting)
            textChanged.set(change)
    }

    private fun incrementBufferVersion() {
        bufferVersion = if (isMaster) bufferVersion.incrementMaster() else bufferVersion.incrementSlave()
    }

    override fun fire(value: RdTextChange) {
        require(delegatedBy.isBound || bufferVersion == TextBufferVersion.INIT_VERSION)
        if (delegatedBy.isBound) protocol.scheduler.assertThread()
        val activeSession = activeTypingSession
        if (activeSession != null && activeSession.isCommitting) {
            return
        }

        incrementBufferVersion()
        val bufferChange = RdTextBufferChange(bufferVersion, localOrigin, value)
        if (value.kind == RdTextChangeKind.Reset) {
            changesToConfirmOrRollback.clear()
        } else if (!isMaster) {
            changesToConfirmOrRollback.add(bufferChange)
        }

        activeSession?.tryPushLocalChange(bufferChange)

        _historyChanged.fire(value)
        sendChange(bufferChange)
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
        if (activeTypingSession != null) return
        val assertion = RdAssertion(bufferVersion.master, bufferVersion.slave, allText)
        val assertedTextProp = if (isMaster) delegatedBy.assertedMasterText else delegatedBy.assertedSlaveText
        assertedTextProp.set(assertion)
    }

    override fun toString(): String {
        return "Text Buffer: ($rdid)"
    }

    override fun startTypingSession(): ITypingSession<RdTextChange> {
        require(activeTypingSession == null) { "activeTypingSession == null" }
        val session = TextBufferTypingSession(this)
        activeTypingSession = session
        return session
    }

    override fun finishTypingSession() {
        require(activeTypingSession != null) { "activeTypingSession != null" }
        activeTypingSession = null
    }

    internal open fun promoteLocalVersion() {
        require(isMaster) { "isMaster" }
        fire(RdTextChange(RdTextChangeKind.PromoteVersion, 0, "", "", -1))
    }

    internal fun updateHistory(initialVersion: TextBufferVersion, versionsToDiscard: List<TextBufferVersion>, changesToApply: List<RdTextBufferChange>) {
        for (version in versionsToDiscard) {
            _discardedBufferVersion.fire(version)
        }

        bufferVersion = initialVersion

        for (change in changesToApply) {
            receiveChange(change)
        }
    }
}
package com.jetbrains.rider.rdtext.impl

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.RdDelegateBase
import com.jetbrains.rider.framework.base.RdReactiveBase.Companion.logReceived
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.rdtext.impl.intrinsics.RdAssertion
import com.jetbrains.rider.rdtext.impl.intrinsics.RdTextBufferChange
import com.jetbrains.rider.rdtext.impl.intrinsics.RdTextBufferState
import com.jetbrains.rider.rdtext.*
import com.jetbrains.rider.rdtext.impl.intrinsics.RdChangeOrigin
import com.jetbrains.rider.rdtext.intrinsics.RdTextChange
import com.jetbrains.rider.rdtext.intrinsics.RdTextChangeKind
import com.jetbrains.rider.rdtext.intrinsics.TextBufferVersion
import com.jetbrains.rider.rdtext.intrinsics.reverse
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.*
import com.jetbrains.rider.util.warn


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

            val activeSession = activeTypingSession
            if (activeSession != null) {
                if (it.version > delegatedBy.versionBeforeTypingSession.valueOrDefault(TextBufferVersion.INIT_VERSION)) {
                    activeSession.pushRemoteChange(it)
                    return@adviseNotNull
                }
            }
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
        textChanged.set(change)
    }

    private fun incrementBufferVersion() {
        bufferVersion = if (isMaster) bufferVersion.incrementMaster() else bufferVersion.incrementSlave()
    }

    override fun fire(value: RdTextChange) {
        require(delegatedBy.isBound || bufferVersion == TextBufferVersion.INIT_VERSION)
        if (delegatedBy.isBound) protocol.scheduler.assertThread()

        val oldVersion = bufferVersion
        incrementBufferVersion()
        val bufferChange = RdTextBufferChange(bufferVersion, localOrigin, value)
        if (value.kind == RdTextChangeKind.Reset) {
            changesToConfirmOrRollback.clear()
        } else if (!isMaster) {
            changesToConfirmOrRollback.add(bufferChange)
        }

        val activeSession = activeTypingSession
        if (activeSession != null) {
            activeSession.pushLocalChange(bufferChange, oldVersion)
        }
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

    override fun startTypingSession(lifetime: Lifetime): ITypingSession {
        require(activeTypingSession == null)
        require(!lifetime.isTerminated)
        val session = TextBufferTypingSession(this, lifetime)
        activeTypingSession = session
        lifetime.add { activeTypingSession = null }
        return session
    }

    private fun rollbackLocalChange(change: RdTextBufferChange, prevVersion: TextBufferVersion) {
        val reversedChange = change.change.reverse()
        bufferVersion = prevVersion
        _discardedBufferVersion.fire(change.version)
        textChanged.set(reversedChange)
    }

    protected open fun promoteLocalVersion() {
        fire(RdTextChange(RdTextChangeKind.PromoteVersion, 0, "", "", -1))
    }

    private class TextBufferTypingSession(private val textBuffer: RdTextBuffer, lifetime: Lifetime) : ITypingSession {
        private val localChangeWithPrevVersions = mutableListOf<Pair<RdTextBufferChange, TextBufferVersion>>()
        private var remoteChanges = mutableListOf<RdTextBufferChange>()
        private var isLocalCommitted = true
        override val onRemoteChange = Signal<ITypingSession.IRemoteChange>()

        init {
            if (textBuffer.isMaster) {
                textBuffer.delegatedBy.versionBeforeTypingSession.set(textBuffer.bufferVersion)
                lifetime.add {
                    if (isLocalCommitted) {
                        // rollback of slave version is asynchronous, that's why promote master version to protect from late changes
                        textBuffer.promoteLocalVersion()
                    }
                }
            }
        }

        override fun rollbackLocalChanges() {
            isLocalCommitted = false
            for ((change, prevVersion) in localChangeWithPrevVersions.asReversed()) {
                textBuffer.rollbackLocalChange(change, prevVersion)
            }
            localChangeWithPrevVersions.clear()
        }

        fun pushLocalChange(change: RdTextBufferChange, prevVersion: TextBufferVersion) {
            localChangeWithPrevVersions.add(change to prevVersion)
        }

        fun pushRemoteChange(change: RdTextBufferChange) {
            onRemoteChange.fire(TextBufferRemoteChange(remoteChanges.size, this))
            remoteChanges.add(change)
        }

        private fun commitRemoteChange(changeIdx: Int) {
            require(localChangeWithPrevVersions.size == 0, { "Before committing remote changes all local changes must be discarded." })
            require(changeIdx < remoteChanges.size, { "changeIdx >= remoteChanges.size" })

            val change = remoteChanges[changeIdx]
            textBuffer.receiveChange(change)
        }

        private class TextBufferRemoteChange(private val idx: Int, override val source: TextBufferTypingSession) : ITypingSession.IRemoteChange {
            override fun commit() = source.commitRemoteChange(idx)
        }
    }
}
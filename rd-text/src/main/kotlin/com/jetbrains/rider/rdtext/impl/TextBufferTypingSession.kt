package com.jetbrains.rider.rdtext.impl

import com.jetbrains.rider.rdtext.ITypingSession
import com.jetbrains.rider.rdtext.impl.intrinsics.RdTextBufferChange
import com.jetbrains.rider.rdtext.intrinsics.RdTextChange
import com.jetbrains.rider.rdtext.intrinsics.TextBufferVersion
import com.jetbrains.rider.rdtext.intrinsics.isNormalChange
import com.jetbrains.rider.util.reactive.Signal
import com.jetbrains.rider.util.reactive.valueOrThrow

internal class TextBufferTypingSession(private val buffer: RdTextBuffer) : ITypingSession<RdTextChange> {
    private enum class State { Opened, Committing, Closing }
    private val versionBeforeOpening: TextBufferVersion
    private var state: State = State.Opened
    private val localVersionToConfirmOrRollback = mutableListOf<TextBufferVersion>()
    private val remoteChanges = mutableListOf<RdTextBufferChange>()
    private val initialVersion = buffer.bufferVersion

    override val onRemoteChange = Signal<RdTextChange>()
    override val onLocalChange = Signal<RdTextChange>()
    val isCommitting: Boolean
        get() = state == State.Committing

    init {
        if (buffer.isMaster) {
            // typing session might start in the middle of changes suppression (see RdDeferrableTextBuffer),
            // that's why we need to separate changes that're queued before the session start
            versionBeforeOpening = buffer.bufferVersion
            buffer.delegatedBy.versionBeforeTypingSession.set(versionBeforeOpening)
        } else {
            versionBeforeOpening = buffer.delegatedBy.versionBeforeTypingSession.valueOrThrow
        }
    }

    override fun startCommitRemoteVersion() {
        require(state == State.Opened)
        state = State.Committing
    }

    override fun finishCommitRemoteVersion() {
        buffer.updateHistory(initialVersion, localVersionToConfirmOrRollback, remoteChanges)
    }

    override fun rollbackRemoteVersion() {
        require(state == State.Opened)
        state = State.Closing

        if (buffer.isMaster) {
            // version promoting protects from rolled back slave's changes (because rollback operation is async)
            buffer.promoteLocalVersion()
        }
    }

    fun tryPushLocalChange(changeWithVersion: RdTextBufferChange): Boolean {
        if (state != State.Opened) return false

        val change = changeWithVersion.change
        require(change.isNormalChange) { "change.isNormalChange" }
        onLocalChange.fire(change)
        localVersionToConfirmOrRollback.add(changeWithVersion.version)
        return true
    }

    fun tryPushRemoteChange(changeWithVersion: RdTextBufferChange): Boolean {
        if (state != State.Opened) return false
        if (changeWithVersion.version <= versionBeforeOpening) return false

        val change = changeWithVersion.change
        require(change.isNormalChange) { "change.isNormalChange" }
        onRemoteChange.fire(change)
        remoteChanges.add(changeWithVersion)
        return true
    }
}
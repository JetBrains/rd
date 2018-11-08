package com.jetbrains.rider.rdtext

import com.jetbrains.rider.rdtext.intrinsics.RdTextChange
import com.jetbrains.rider.rdtext.intrinsics.TextBufferVersion
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.ISignal
import com.jetbrains.rider.util.reactive.ISource

interface ITextBuffer : ISignal<RdTextChange> {
    /**
     * Indicates if this side has higher priority level than other.
     */
    val isMaster: Boolean
    /**
     * Current buffer version that consists from a pair of master and slave timestamps.
     */
    val bufferVersion: TextBufferVersion

    /**
     * Source of events that have been applied upon a document state ever.
     */
    val historyChanged: ISource<RdTextChange>

    /**
     * Resets TextBuffer state and sends event to replace a whole text on the opposite side.
     * Used fot setting initial text too.
     */
    fun reset(text: String)

    /**
     * Asserts that both sides have the same document text if buffer versions are same.
     */
    fun assertState(allText: String)
}

interface ITextBufferWithTypingSession : ITextBuffer {
    val discardedBufferVersion: ISource<TextBufferVersion>

    fun startTypingSession(): ITypingSession<RdTextChange>
    fun finishTypingSession()
}

interface ITypingSession<TChange> {
    fun startCommitRemoteVersion()
    fun finishCommitRemoteVersion()
    fun rollbackRemoteVersion()
    val onRemoteChange: ISource<TChange>
    val onLocalChange: ISource<TChange>
}

/**
 * for completion purpose
 */
interface IDeferrableITextBuffer : ITextBuffer {
    /**
     * Calls [fire] method, but postpones sending events to protocol until [flush] is called.
     */
    fun queue(newChange: RdTextChange)

    /**
     * Sends all postponed events to protocol.
     */
    fun flush()

    val isQueueEmpty: Boolean
}
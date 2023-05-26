package com.jetbrains.rd.rdtext.impl

import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.framework.base.deepClonePolymorphic
import com.jetbrains.rd.framework.protocolOrThrow
import com.jetbrains.rd.rdtext.IDeferrableITextBuffer
import com.jetbrains.rd.rdtext.intrinsics.RdTextChange
import com.jetbrains.rd.rdtext.intrinsics.RdTextChangeKind
import com.jetbrains.rd.rdtext.impl.intrinsics.RdTextBufferChange
import com.jetbrains.rd.rdtext.impl.intrinsics.RdTextBufferState
import com.jetbrains.rd.util.debug
import com.jetbrains.rd.util.reflection.usingTrueFlag

class RdDeferrableTextBuffer(delegate: RdTextBufferState, isMaster: Boolean = true) : RdTextBuffer(delegate, isMaster), IDeferrableITextBuffer {
    private val queue = mutableListOf<RdTextBufferChange>()
    private var isQueueing = false

    override fun receiveChange(textBufferChange: RdTextBufferChange) {
        if (textBufferChange.change.kind == RdTextChangeKind.Reset) {
            queue.clear()
        }
        super.receiveChange(textBufferChange)
    }

    override fun sendChange(value: RdTextBufferChange) {
        if (value.change.kind == RdTextChangeKind.Reset) {
            queue.clear()
            super.sendChange(value)
            return
        }

        if (isQueueing) {
            queue.add(value)
        } else {
            if (queue.isNotEmpty()) flush()
            super.sendChange(value)
        }
    }

    override fun flush() {
        protocolOrThrow.scheduler.assertThread()
        require(!queue.isEmpty()) { "!queue.isEmpty()" }
        RdReactiveBase.logSend.debug { "Sending queued changes" }
        try {
            for (e in queue) super.sendChange(e)
        } finally {
            queue.clear()
        }
    }

    override fun queue(newChange: RdTextChange) {
        usingTrueFlag(RdDeferrableTextBuffer::isQueueing) {
            fire(newChange)
        }
    }

    override fun promoteLocalVersion() {
        usingTrueFlag(RdDeferrableTextBuffer::isQueueing, queue.isNotEmpty()) {
            super.promoteLocalVersion()
        }
    }

    override val isQueueEmpty get() = queue.isEmpty()

    override fun deepClone(): IRdBindable {
        return RdDeferrableTextBuffer(delegatedBy.deepClonePolymorphic(), isMaster)
    }
}

@Deprecated("Use normal buffer as backend counterpart", ReplaceWith("rdSlaveTextBuffer(delegate)"))
fun rdDeferrableSlaveTextBuffer(delegate: RdTextBufferState) = rdSlaveTextBuffer(delegate)

fun rdSlaveTextBuffer(delegate: RdTextBufferState) = RdTextBuffer(delegate, false)

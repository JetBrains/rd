package com.jetbrains.rider.framework.text.impl

import com.jetbrains.rider.framework.base.RdReactiveBase
import com.jetbrains.rider.framework.text.IDeferrableITextBuffer
import com.jetbrains.rider.framework.text.RdTextChange
import com.jetbrains.rider.framework.text.RdTextChangeKind
import com.jetbrains.rider.framework.text.intrinsics.RdTextBufferChange
import com.jetbrains.rider.framework.text.intrinsics.RdTextBufferState
import com.jetbrains.rider.util.debug
import com.jetbrains.rider.util.reflection.usingTrueFlag

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
        protocol.scheduler.assertThread()
        require(!queue.isEmpty())
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

    override val isValid: Boolean get() = delegatedBy.isBound
}

fun rdDeferrableSlaveTextBuffer(delegate: RdTextBufferState) = RdDeferrableTextBuffer(delegate, false)

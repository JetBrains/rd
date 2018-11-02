package com.jetbrains.rider.framework.base

import com.jetbrains.rider.framework.AbstractBuffer
import com.jetbrains.rider.util.reactive.IScheduler

/**
 * A non-root node in an object graph which can be synchronized with its remote copy over a network or a similar connection,
 * and which allows to subscribe to its changes.
 */
interface IRdReactive : IRdBindable {
    /**
     * If set to true, local changes to this object can be performed on any thread.
     * Otherwise, local changes can be performed only on the UI thread.
     */
    var async: Boolean

    /**
     * Scheduler on which wire invokes callback [onWireReceived]. Default is the same as [protocol]'s one.
     */
    val wireScheduler : IScheduler

    /**
     * Callback that wire triggers when it receives messaged
     */
    fun onWireReceived(buffer: AbstractBuffer)
}


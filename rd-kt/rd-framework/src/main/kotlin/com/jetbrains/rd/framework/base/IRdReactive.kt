package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.framework.IRdDynamic
import com.jetbrains.rd.framework.RdId
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler

/**
 * A non-root node in an object graph which can be synchronized with its remote copy over a network or a similar connection,
 * and which allows to subscribe to its changes.
 */
interface IRdReactive : IRdBindable, IRdWireable {
    /**
     * If set to true, local changes to this object can be performed on any thread.
     * Otherwise, local changes can be performed only on the UI thread.
     */
    var async: Boolean
}

/**
 * Entity that could receive messages from wire
 */
interface IRdWireable: IRdDynamic {

    val rdid: RdId

    /**
     * Callback that wire triggers when it receives messaged
     */
    fun onWireReceived(buffer: AbstractBuffer, dispatchHelper: IRdWireableDispatchHelper)
}

interface IRdWireableDispatchHelper {
    val rdId: RdId
    val lifetime: Lifetime

    fun dispatch(scheduler: IScheduler? = null, action: () -> Unit)
}



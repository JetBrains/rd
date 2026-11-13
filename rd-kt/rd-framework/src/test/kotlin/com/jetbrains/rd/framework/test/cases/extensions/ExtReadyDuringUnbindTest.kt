package com.jetbrains.rd.framework.test.cases.extensions

import com.jetbrains.rd.framework.RdId
import com.jetbrains.rd.framework.base.IRdWireableDispatchHelper
import com.jetbrains.rd.framework.base.RdExtBase
import com.jetbrains.rd.framework.createAbstractBuffer
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.framework.writeEnum
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import demo.demoModel
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class ExtReadyDuringUnbindTest : RdFrameworkTestBase() {
    @Test
    fun testNoReplyToReadyOnceTheExtLifetimeTerminates() {
        // a top-level ext writes to the test wire directly, a nested one queues until its parent connects
        val ext = clientProtocol.demoModel
        val extLifetime = assertNotNull(ext.bindLifetime)

        assertNotEquals(0L, bytesSentOnReady(ext, extLifetime), "a bound ext answers the counterpart's Ready")

        // The wire thread can dispatch Ready while another thread unbinds the ext.
        // This action runs first in the termination, while the ext is still bound.
        var bytesSentDuringTermination = -1L
        clientLifetime.onTermination {
            assertFalse(ext.rdid.isNull, "precondition: the ext is still bound")
            bytesSentDuringTermination = bytesSentOnReady(ext, extLifetime)
        }
        clientLifetimeDef.terminate()

        assertEquals(0L, bytesSentDuringTermination, "an ext that is being unbound must not answer Ready")
    }

    private fun bytesSentOnReady(ext: RdExtBase, lifetime: Lifetime): Long {
        val message = createAbstractBuffer().apply {
            writeEnum(RdExtBase.ExtState.Ready)
            writeLong(ext.serializationHash)
            rewind()
        }
        val before = clientWire.bytesWritten
        // In the race, the wire thread passes the lifetime check of the entry point before the unbind starts,
        // so the test calls the handler behind that check directly
        ext.onWireReceived(clientProtocol, message, clientProtocol.serializationContext, DispatchHelper(ext.rdid, lifetime))
        return clientWire.bytesWritten - before
    }

    private class DispatchHelper(override val rdId: RdId, override val lifetime: Lifetime) : IRdWireableDispatchHelper {
        override fun dispatch(scheduler: IScheduler?, action: () -> Unit) = action()
    }
}

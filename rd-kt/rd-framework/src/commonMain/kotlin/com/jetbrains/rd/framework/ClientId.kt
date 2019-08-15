package com.jetbrains.rd.framework

import com.jetbrains.rd.util.threadLocalWithInitial
import kotlinx.coroutines.Runnable
import kotlin.jvm.JvmStatic

class ClientId(val id: String) {
    companion object : ISerializer<ClientId> {
        private val defaultLocalId = ClientId("Host")

        @JvmStatic
        var localId = defaultLocalId
            get
            private set

        @JvmStatic
        val isCurrentlyUnderLocalId: Boolean
            get() = isLocalId(current)

        private val currentClientId = threadLocalWithInitial<ClientId?> { null }

        @JvmStatic
        val current: ClientId?
            get() = currentClientId.get()

        @JvmStatic
        fun overrideLocalId(newId: ClientId) {
            require(localId == defaultLocalId)
            localId = newId
        }

        @JvmStatic
        fun isLocalId(clientId: ClientId?): Boolean {
            return clientId == localId || clientId == null
        }

        @JvmStatic
        fun withClientId(clientId: ClientId?, action: Runnable) {
            val oldClientId = currentClientId.get()
            try {
                currentClientId.set(clientId)
                action.run()
            } finally {
                currentClientId.set(oldClientId)
            }
        }

        @JvmStatic
        fun <T> withClientId(clientId: ClientId?, action: () -> T): T {
            val oldClientId = currentClientId.get()
            try {
                currentClientId.set(clientId)
                return action()
            } finally {
                currentClientId.set(oldClientId)
            }
        }

        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ClientId {
            return ClientId(buffer.readString())
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ClientId) {
            buffer.writeString(value.id)
        }
    }
}
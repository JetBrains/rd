package com.jetbrains.rd.framework

import com.jetbrains.rd.util.Callable
import com.jetbrains.rd.util.Runnable
import com.jetbrains.rd.util.reflection.usingValue
import kotlin.jvm.JvmStatic

/**
 * ClientId is a global context class that is used to distinguish the originator of an action in multi-client systems
 * In such systems, each client has their own ClientId. Current process also can have its own ClientId, with this class providing methods to distinguish local actions from remote ones.
 *
 * It's up to the application to preserve and propagate the current value across background threads and asynchronous activities.
 */
data class ClientId(val value: String) {
    enum class AbsenceBehavior {
        /**
         * Return localId if ClientId is not set
         */
        RETURN_LOCAL,
        /**
         * Throw an exception of ClientId is not set
         */
        THROW
    }

    companion object : ISerializer<ClientId> {
        private val defaultLocalId = ClientId("Host")
        val contextKey = RdContext("ClientId", true, FrameworkMarshallers.String)

        /**
         * Specifies behavior for ClientId.current
         */
        var AbsenceBehaviorValue = AbsenceBehavior.RETURN_LOCAL

        /**
         * The ID considered local to this process. All other IDs (except for null) are considered remote
         */
        @JvmStatic
        var localId = defaultLocalId
            get
            private set

        /**
         * True if and only if the current ClientID is local to this process
         */
        @JvmStatic
        val isCurrentlyUnderLocalId: Boolean
            get() = isLocalId(current)

        /**
         * Gets the current ClientId. Subject to AbsenceBehaviorValue
         */
        @JvmStatic
        val current: ClientId
            get() = when(AbsenceBehaviorValue) {
                AbsenceBehavior.RETURN_LOCAL -> currentOrNull ?: localId
                AbsenceBehavior.THROW -> currentOrNull ?: throw NullPointerException("ClientId not set")
            }

        /**
         * Gets the current ClientId. Can be null if non was set.
         */
        @JvmStatic
        val currentOrNull: ClientId?
            get() = contextKey.value?.let(::ClientId)

        /**
         * Overrides the ID that is considered to be local to this process. Can be only invoked once.
         */
        @JvmStatic
        fun overrideLocalId(newId: ClientId) {
            require(localId == defaultLocalId)
            localId = newId
        }

        /**
         * Returns true if and only if the given ID is considered to be local to this process
         */
        @JvmStatic
        fun isLocalId(clientId: ClientId?): Boolean {
            return clientId == localId || clientId == null
        }

        /**
         * Invokes a runnable under the given ClientId
         */
        @JvmStatic
        fun withClientId(clientId: ClientId?, action: Runnable) = withClientId(clientId) { action.run() }

        /**
         * Computes a value under given ClientId
         */
        @JvmStatic
        fun <T> withClientId(clientId: ClientId?, action: Callable<T>): T = withClientId(clientId) { action.call() }

        /**
         * Computes a value under given ClientId
         */
        @JvmStatic
        inline fun <T> withClientId(clientId: ClientId?, action: () -> T): T = contextKey::value.usingValue(clientId?.value, action)

        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ClientId {
            return ClientId(buffer.readString())
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ClientId) {
            buffer.writeString(value.value)
        }
    }
}
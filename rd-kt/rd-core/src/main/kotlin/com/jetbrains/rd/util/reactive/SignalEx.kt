package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.lifetime.Lifetime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.consumeAsFlow

class SignalFlow<T>(private val signal: ISignal<T>) : Flow<T> {

    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        Lifetime.using { lifetime ->
            val channel = Channel<T>(Channel.UNLIMITED)
            signal.advise(lifetime) {
                channel.trySend(it)
            }

            channel.consumeAsFlow().collect(collector)
        }

        error("Unreachable")
    }
}

fun <T> ISignal<T>.asFlow() = SignalFlow(this)
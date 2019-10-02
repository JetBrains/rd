package com.jetbrains.rd.util.threading

import com.jetbrains.rd.util.catch
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.receiveOrNull

@ExperimentalCoroutinesApi
class QueueProcessor<T : Any>(val lifetime: Lifetime, process: suspend (T) -> Unit) {
    private val channel = Channel<T>(UNLIMITED)

    init {
        lifetime.onTerminationIfAlive {
            channel.close()
        }

        GlobalScope.launch {
            while (lifetime.isAlive) {
                val item = channel.receiveOrNull() ?: break
                catch { process(item) }
            }
        }
    }


    fun queue(item: T) = lifetime.executeIfAlive {
        runBlocking {
            channel.send(item)
        }
    }
}
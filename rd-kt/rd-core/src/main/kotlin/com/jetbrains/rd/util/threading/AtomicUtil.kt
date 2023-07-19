package com.jetbrains.rd.util.threading

import com.jetbrains.rd.util.AtomicInteger
import com.jetbrains.rd.util.DelicateRdApi
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.lifetime.isNotAlive
import com.jetbrains.rd.util.reactive.*
import java.lang.UnsupportedOperationException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@DelicateRdApi
interface AdviseToAdviseOnSynchronizer {
    fun acquireReadCookie(lifetime: Lifetime): Boolean
    fun releaseReadCookie()
    fun acquireUpdateCookie()
    fun releaseUpdateCookie()
}

@DelicateRdApi
class AdviseToAdviseOnSynchronizerImpl : AdviseToAdviseOnSynchronizer, AtomicInteger(0) {
    override fun acquireUpdateCookie() {
        while (true) {
            val value = get()
            if (value >= 0 && compareAndSet(value, value + 1))
                break

            if (Thread.interrupted())
                throw InterruptedException()

            Thread.onSpinWait()
        }
    }

    override fun releaseUpdateCookie() {
        decrementAndGet()
    }

    override fun acquireReadCookie(lifetime: Lifetime): Boolean {
        while (true) {
            val value = get()
            if (value <= 0 && compareAndSet(value, value - 1))
                return true

            if (lifetime.isNotAlive)
                return false

            if (Thread.interrupted())
                throw InterruptedException()

            Thread.onSpinWait()
        }
    }

    override fun releaseReadCookie() {
        incrementAndGet()
    }

    override fun toShort(): Short = throw UnsupportedOperationException()
    override fun toByte(): Byte = throw UnsupportedOperationException()
    override fun toChar(): Char = throw UnsupportedOperationException()
}

@DelicateRdApi
object ThreadSafeAdviseToAdviseOnSynchronizer : AdviseToAdviseOnSynchronizer {

    override fun acquireUpdateCookie() = Unit
    override fun releaseUpdateCookie() = Unit

    override fun acquireReadCookie(lifetime: Lifetime) = lifetime.isAlive
    override fun releaseReadCookie() = Unit

    fun <T, R> toMappedProperty(sourceProperty: IPropertyView<T>, lifetime: Lifetime, map: (T) -> R): Property<R> {
        while (true) {
            val nestedDefinition = lifetime.createNested()
            try {
                val resultProperty = Property(map(sourceProperty.value))
                val count = AtomicInteger()

                sourceProperty.change.advise(nestedDefinition.lifetime) {
                    count.incrementAndGet()
                    resultProperty.set(map(it))
                }

                resultProperty.set(map(sourceProperty.value))

                if (count.get() == 0)
                    return resultProperty

                nestedDefinition.terminate()

            } catch (e: Throwable) {
                nestedDefinition.terminate()
                throw e
            }
        }
    }

    fun <T : Any, R : Any> toMappedOptProperty(sourceProperty: IOptPropertyView<T>, lifetime: Lifetime, map: (T) -> R): OptProperty<R> {
        while (true) {
            val nestedDefinition = lifetime.createNested()
            try {
                val resultProperty = OptProperty<R>()
                val count = AtomicInteger()

                sourceProperty.change.advise(nestedDefinition.lifetime) {
                    count.incrementAndGet()
                    resultProperty.set(map(it))
                }

                val value = sourceProperty.valueOrNull
                if (value != null)
                    resultProperty.set(map(value))

                if (count.get() == 0)
                    return resultProperty

                nestedDefinition.terminate()

            } catch (e: Throwable) {
                nestedDefinition.terminate()
                throw e
            }
        }
    }
}

@DelicateRdApi
@OptIn(ExperimentalContracts::class)
inline fun <T> AdviseToAdviseOnSynchronizer.modifyAndFireChanges(signal: Signal<T>, action: () -> List<T>?) {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }

    acquireUpdateCookie()
    val value = try {
        action()
    } catch (e: Throwable) {
        releaseReadCookie()
        throw e
    }
    signal.takeSnapshotAndFireChanges {
        releaseUpdateCookie()
        value
    }
}

@DelicateRdApi
fun <T> AdviseToAdviseOnSynchronizer.adviseOn(source: ISource<T>, lifetime: Lifetime, scheduler: IScheduler, handler: (T) -> Unit) {
    if (!acquireReadCookie(lifetime))
        return

    try {
        source.advise(lifetime) {
            scheduler.queue { handler(it) }
        }
    } finally {
        releaseReadCookie()
    }
}

@DelicateRdApi
@OptIn(ExperimentalContracts::class)
inline fun <T> AdviseToAdviseOnSynchronizer.modifyAndFireChange(signal: Signal<T>, action: () -> T) {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }

    acquireUpdateCookie()
    val value = try {
        action()
    } catch (e: Throwable) {
        releaseReadCookie()
        throw e
    }
    signal.takeSnapshotAndFireChange {
        releaseUpdateCookie()
        value
    }
}

package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.DelicateRdApi
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.jetbrains.rd.util.threading.ThreadSafeAdviseToAdviseOnSynchronizer
import com.jetbrains.rd.util.threading.adviseOn
import java.util.concurrent.atomic.AtomicInteger

fun <T, K> IPropertyView<T>.switchMap(f: (T) -> IPropertyView<K>) = object : IPropertyView<K> {

    override val change: ISource<K> = object : ISource<K> {
        override fun advise(lifetime: Lifetime, handler: (K) -> Unit) {

            while (true) {
                val nestedDefinition = lifetime.createNested()
                try {
                    val property = Property(value)
                    val count = AtomicInteger()

                    val lifetimes = SequentialLifetimes(nestedDefinition.lifetime)
                    val firstLifetime = lifetimes.next()

                    this@switchMap.change.advise(nestedDefinition.lifetime) {
                        count.incrementAndGet()

                        f(it).advise(lifetimes.next()) {
                            property.set(it)
                        }
                    }

                    f(this@switchMap.value).change.advise(firstLifetime) {
                        count.incrementAndGet()

                        property.set(it)
                    }

                    property.set(value)

                    if (count.get() == 0) {
                        property.change.advise(nestedDefinition.lifetime) { handler(it) }
                        return
                    }

                    nestedDefinition.terminate()

                } catch (e: Throwable) {
                    nestedDefinition.terminate()
                    throw e
                }
            }
        }

        override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (K) -> Unit) {
            @OptIn(DelicateRdApi::class)
            ThreadSafeAdviseToAdviseOnSynchronizer.adviseOn(this, lifetime, scheduler, handler)
        }
    }

    override val value: K
        get() = f(this@switchMap.value).value
}
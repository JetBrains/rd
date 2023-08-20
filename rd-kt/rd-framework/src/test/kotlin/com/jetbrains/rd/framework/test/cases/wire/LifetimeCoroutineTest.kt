package com.jetbrains.rd.framework.test.cases.wire

import com.jetbrains.rd.framework.test.cases.CoroutineTestBase
import com.jetbrains.rd.util.AtomicReference
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.LifetimeStatus
import com.jetbrains.rd.util.spinUntil
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import kotlin.test.assertEquals

class LifetimeCoroutineTest : CoroutineTestBase() {
    @Test
    fun terminateSuspendingTest() = runBlocking {
            val testScope = this
            var count = 0

            fun Lifetime.checkScopeCancellation() {
                val lifetimedScope = coroutineScope
                // there is no guarantee that cancellation will be observed immediately,
                // but we want to check that scope is already cancelled
                spinUntil(1000) { !lifetimedScope.isActive }
                val toString = lifetimedScope.toString()
                if (lifetimedScope.isActive)
                    testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        throw IllegalStateException("Scope must not be alive during termination: $toString")
                    }
            }

            fun LifetimeDefinition.addOnTermination() {
                onTerminationIfAlive {
                    checkScopeCancellation()
                }
            }

            fun newDef(): LifetimeDefinition = LifetimeDefinition().apply {
                id = count++
                addOnTermination()
            }

            val atomicDefinition = AtomicReference(newDef().apply { terminate() })

            val terminationJob = launch(start = CoroutineStart.UNDISPATCHED) {
                (0..5).forEach {
                    launch(Dispatchers.Default, CoroutineStart.UNDISPATCHED) {
                        while (isActive) {
                            atomicDefinition.get().terminate()
                            yield()
                        }
                    }
                }

                (0..5).forEach {
                    launch(Dispatchers.Default, CoroutineStart.UNDISPATCHED) {
                        while (isActive) {
                            val definition = atomicDefinition.get()
                            definition.terminateSuspending(false)
                            yield()
                        }
                    }
                }

                (0..5).forEach {
                    launch(Dispatchers.Default, CoroutineStart.UNDISPATCHED) {
                        while (isActive) {
                            val definition = atomicDefinition.get()
                            definition.awaitTermination()
                            yield()
                        }
                    }
                }
            }

            for (i in 0..1_00_000) {
                val definition = newDef()
                atomicDefinition.getAndSet(definition)
                yield()
                definition.terminate()
            }

            terminationJob.cancel()
        }

    @Test
    fun awaitCancellationTest() = runBlocking {
        val testScope = this
        var count = 0

        fun Lifetime.checkScopeCancellation() {
            val lifetimedScope = coroutineScope
            // there is no guarantee that cancellation will be observed immediately,
            // but we want to check that scope is already cancelled
            spinUntil(1000) { !lifetimedScope.isActive }
            val toString = lifetimedScope.toString()
            if (lifetimedScope.isActive)
                testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    throw IllegalStateException("Scope must not be alive during termination: $toString")
                }
        }

        fun LifetimeDefinition.addOnTermination() {
            onTerminationIfAlive {
                checkScopeCancellation()
            }
        }

        fun newDef(): LifetimeDefinition = LifetimeDefinition().apply {
            id = count++
            addOnTermination()
        }

        val atomicDefinition = AtomicReference(newDef().apply { terminate() })

        val terminationJob = launch(start = CoroutineStart.UNDISPATCHED) {

            (0..5).forEach {
                launch(Dispatchers.Default, CoroutineStart.UNDISPATCHED) {
                    while (isActive) {
                        val definition = atomicDefinition.get()
                        definition.awaitTermination()
                        assertEquals(LifetimeStatus.Terminated,definition.status)
                        yield()
                    }
                }

                launch(Dispatchers.Default, CoroutineStart.UNDISPATCHED) {
                    while (isActive) {
                        val definition = atomicDefinition.get()
                        val scope = definition.coroutineScope
                        definition.awaitTermination()
                        assertEquals(LifetimeStatus.Terminated,definition.status)
                        assert(scope.coroutineContext.job.isCompleted) {
                            scope.toString()
                        }
                        yield()
                    }
                }

            }
        }

        for (i in 0..1_00_000) {
            val definition = newDef()
            atomicDefinition.getAndSet(definition)
            yield()
            if (i % 2 == 0)
                definition.terminate()
            else
                definition.terminateSuspending(true)
        }

        terminationJob.cancel()
    }


}
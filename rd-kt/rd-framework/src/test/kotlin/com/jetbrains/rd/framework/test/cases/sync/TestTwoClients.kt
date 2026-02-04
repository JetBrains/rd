package com.jetbrains.rd.framework.test.cases.sync

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.test.util.SequentialPumpingScheduler
import com.jetbrains.rd.framework.test.util.TestBase
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.reactive.hasValue
import com.jetbrains.rd.util.reactive.valueOrThrow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import test.synchronization.*

private val serverId = AtomicInteger()

class TestTwoClients : TestBase() {


    lateinit var c0: SyncModelExt
    lateinit var c1: SyncModelExt
    lateinit var s0: SyncModelExt
    lateinit var s1: SyncModelExt

    @BeforeEach
    fun setup() {
        ConsoleLoggerFactory.traceCategories.addAll(listOf("protocol", TestTwoClients::class.qualifiedName!!))

        val sc = SequentialPumpingScheduler

        val parametersFactory = {
            val newId = serverId.incrementAndGet()
            SocketWire.WireParameters(sc, "ServerSocket[$newId]")
        }
        val wireFactory = SocketWire.ServerFactory(lifetime, parametersFactory, null, false)
        val port = wireFactory.localPort

        val sp = mutableListOf<Protocol>()
        wireFactory.view(lifetime) { lf, wire ->
            val protocol = Protocol("${wire.id}.Protocol", Serializers(), SequentialIdentities(IdKind.Server), sc, wire, lf, SyncModelExt.ClientId)
            sp.addUnique(lf, protocol)
        }


        var cpIdx = 0
        val cpFunc = { Protocol("c[${cpIdx++}]", Serializers(), SequentialIdentities(IdKind.Client), sc, SocketWire.Client(lifetime, sc, port), lifetime, SyncModelExt.ClientId) }

        val cp = mutableListOf<Protocol>()
        cp.add(cpFunc())
        cp.add(cpFunc())

        wait { sp.size  == 2 }

        c0 = cp[0].syncModelExt
        c1 = cp[1].syncModelExt

        s0 = sp[0].syncModelExt
        s1 = sp[1].syncModelExt

        s0.synchronizeWith(lifetime, s1)
    }

    @AfterEach
    fun teardown() {
        ConsoleLoggerFactory.traceCategories.clear()
    }

    @Test
    fun testNotnullableScalarProperty() {
        c0.aggregate.nonNullableScalarProperty.set("a")
        wait { c1.aggregate.nonNullableScalarProperty.valueOrNull == "a" }

        c1.aggregate.nonNullableScalarProperty.set("b")
        wait { c0.aggregate.nonNullableScalarProperty.valueOrNull == "b" }
    }


    @Test
    fun testNullableScalarProperty() {
        c0.aggregate.nullableScalarProperty.set("a")
        wait { c1.aggregate.nullableScalarProperty.value == "a" }

        c1.aggregate.nullableScalarProperty.set("b")
        wait { c0.aggregate.nullableScalarProperty.value == "b" }

        c1.aggregate.nullableScalarProperty.set(null)
        wait { c0.aggregate.nullableScalarProperty.value == null }
    }

    @Test
    fun testList() {
        c0.list.add(Clazz(1))
        wait { c1.list.size == 1 }
        assert(c1.list[0].f == 1)

        c0.list[0].p.value = 2
        wait { c1.list[0].p.value == 2 }
    }

    @Test
    fun testDict() {
        c0.map.put(0, Clazz(1))
        wait { c1.map.size == 1 }
        assert(c1.map[0]?.f == 1)

        c0.map[0]?.p?.value = 2
        wait { c1.map[0]?.p?.value == 2 }
    }

    @Test
    fun testPerClientIdMap() {
        SyncModelExt.ClientId.updateValue("Host").use {
            c0.property.set(Clazz(1))
            wait { c1.property.hasValue }

            c0.property.valueOrThrow.mapPerClientId[1] = 1
            wait { c1.property.valueOrThrow.mapPerClientId[1] == 1 }
        }
    }

    @Test
    fun testPerClientIdMapIntersection() {
        c0.property.set(Clazz(1))
        wait { c1.property.hasValue }

        val c0ContextSet = listOf("A", "B", "C")
        val c1ContextSet = listOf("C", "D", "E")

        c0.protocolOrThrow.contexts.getValueSet(SyncModelExt.ClientId).addAll(c0ContextSet)
        c1.protocolOrThrow.contexts.getValueSet(SyncModelExt.ClientId).addAll(c1ContextSet)

        SyncModelExt.ClientId.updateValue("C").use {
            c0.property.valueOrThrow.mapPerClientId[1] = 1
            wait { c1.property.valueOrThrow.mapPerClientId[1] == 1 }
        }

        assertEquals(c0ContextSet, c0.property.valueOrThrow.mapPerClientIdPerContextMap.map { it.component1() })
        assertEquals(c1ContextSet, c1.property.valueOrThrow.mapPerClientIdPerContextMap.map { it.component1() })
    }


    @Test
    fun testPerClientIdProperty() {
        SyncModelExt.ClientId.updateValue("Host").use {
            c0.protocolOrThrow.contexts.getValueSet(SyncModelExt.ClientId).add("Host")
            c1.protocolOrThrow.contexts.getValueSet(SyncModelExt.ClientId).add("Host")

            wait { s0.protocolOrThrow.contexts.getValueSet(SyncModelExt.ClientId).contains("Host") }
            wait { s1.protocolOrThrow.contexts.getValueSet(SyncModelExt.ClientId).contains("Host") }

            c0.propPerClientId.set(1)
            wait { c1.propPerClientId.valueOrNull == 1 }
        }
    }

    @Test
    fun testProperty() {
        c0.property.set(Clazz(1))
        wait { c1.property.hasValue }

        c0.property.valueOrThrow.p.value = 2
        wait { c1.property.valueOrThrow.p.value == 2 }
    }


    @Test
    fun testExt() {
        c0.property.set(Clazz(1))
        wait { c1.property.hasValue }

        val myLogger = getLogger<TestTwoClients>()

        myLogger.trace {"\nSTART---------------------------------------------------------------"}
        c0.property.valueOrThrow.extToClazz.map[0] = Clazz(2)

        myLogger.trace {"\n1------------------------------------------------------------------"}
        s0.property.valueOrThrow.extToClazz //just create

        myLogger.trace {"\n2------------------------------------------------------------------"}
        //s1.property.valueOrThrow.extToClazz must be created automatically
        wait { c1.property.valueOrThrow.extToClazz.map[0]?.f == 2 }

        myLogger.trace {"\n3------------------------------------------------------------------"}
        c1.property.valueOrThrow.extToClazz.map[0]?.p?.value = 3
        wait { c1.property.valueOrThrow.extToClazz.map[0]?.p?.value == 3 }

        myLogger.trace {"\nFINISH---------------------------------------------------------------"}
    }
}
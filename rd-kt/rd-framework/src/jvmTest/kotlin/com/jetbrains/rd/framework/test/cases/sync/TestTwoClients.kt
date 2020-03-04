package com.jetbrains.rd.framework.test.cases.sync

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.test.util.SequentialPumpingScheduler
import com.jetbrains.rd.framework.test.util.TestBase
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.reactive.hasValue
import com.jetbrains.rd.util.reactive.valueOrThrow
import com.jetbrains.rd.util.reflection.usingValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import test.synchronization.Clazz
import test.synchronization.SyncModelRoot
import test.synchronization.extToClazz

class TestTwoClients : TestBase() {


    lateinit var c0: SyncModelRoot
    lateinit var c1: SyncModelRoot
    lateinit var s0: SyncModelRoot
    lateinit var s1: SyncModelRoot

    @BeforeEach
    fun setup() {
        ConsoleLoggerFactory.traceCategories.addAll(listOf("protocol", TestTwoClients::class.qualifiedName!!))

        val sc = SequentialPumpingScheduler

        val wireFactory = SocketWire.ServerFactory(lifetime, sc, null, false)
        val port = wireFactory.localPort

        val sp = mutableListOf<Protocol>()
        var spIdx = 0
        wireFactory.view(lifetime) { lf, wire ->
            val protocol = Protocol("s[${spIdx++}]", Serializers(), Identities(IdKind.Server), sc, wire, lf, SyncModelRoot.ClientId)
            sp.addUnique(lf, protocol)
        }


        var cpIdx = 0
        val cpFunc = { Protocol("c[${cpIdx++}]", Serializers(), Identities(IdKind.Client), sc, SocketWire.Client(lifetime, sc, port), lifetime, SyncModelRoot.ClientId) }

        val cp = mutableListOf<Protocol>()
        cp.add(cpFunc())
        cp.add(cpFunc())

        wait { sp.size  == 2 }

        c0 = SyncModelRoot.create(lifetime, cp[0])
        c1 = SyncModelRoot.create(lifetime, cp[1])

        s0 = SyncModelRoot.create(lifetime, sp[0])
        s1 = SyncModelRoot.create(lifetime, sp[1])

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
        SyncModelRoot.ClientId::value.usingValue("Host") {
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

        c0.protocol.contexts.getValueSet(SyncModelRoot.ClientId).addAll(c0ContextSet)
        c1.protocol.contexts.getValueSet(SyncModelRoot.ClientId).addAll(c1ContextSet)

        SyncModelRoot.ClientId::value.usingValue("C") {
            c0.property.valueOrThrow.mapPerClientId[1] = 1
            wait { c1.property.valueOrThrow.mapPerClientId[1] == 1 }
        }

        assertEquals(c0ContextSet, c0.property.valueOrThrow.mapPerClientIdPerContextMap.map { it.component1() })
        assertEquals(c1ContextSet, c1.property.valueOrThrow.mapPerClientIdPerContextMap.map { it.component1() })
    }


    @Test
    fun testPerClientIdProperty() {
        SyncModelRoot.ClientId::value.usingValue("Host") {
            c0.protocol.contexts.getValueSet(SyncModelRoot.ClientId).add("Host")
            c1.protocol.contexts.getValueSet(SyncModelRoot.ClientId).add("Host")

            wait { s0.protocol.contexts.getValueSet(SyncModelRoot.ClientId).contains("Host") }
            wait { s1.protocol.contexts.getValueSet(SyncModelRoot.ClientId).contains("Host") }

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
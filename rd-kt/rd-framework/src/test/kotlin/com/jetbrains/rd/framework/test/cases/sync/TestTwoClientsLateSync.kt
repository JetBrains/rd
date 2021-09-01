package com.jetbrains.rd.framework.test.cases.sync

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.RdBindableBase
import com.jetbrains.rd.framework.test.util.SequentialPumpingScheduler
import com.jetbrains.rd.framework.test.util.TestBase
import com.jetbrains.rd.util.ConsoleLoggerFactory
import com.jetbrains.rd.util.addUnique
import com.jetbrains.rd.util.reactive.hasValue
import com.jetbrains.rd.util.reactive.valueOrThrow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import test.synchronization.Clazz
import test.synchronization.SyncModelRoot
import test.synchronization.extToClazz
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.set
import kotlin.collections.setOf
import kotlin.collections.toPair
import kotlin.collections.toSet

class TestTwoClientsLateSync : TestBase() {


    lateinit var c0: SyncModelRoot
    lateinit var c1: SyncModelRoot
    lateinit var s0: SyncModelRoot
    lateinit var s1: SyncModelRoot

    @BeforeEach
    fun setup() {
        ConsoleLoggerFactory.traceCategories.addAll(listOf("protocol", TestTwoClientsLateSync::class.qualifiedName!!))

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
    }

    private fun doSynchronize(accepts: (Any?) -> Boolean = { true }) {
        s0.synchronizeWith(lifetime, s1, accepts)
    }

    @AfterEach
    fun teardown() {
        ConsoleLoggerFactory.traceCategories.clear()
    }

    @Test
    fun testNotnullableScalarProperty() {
        s0.aggregate.nonNullableScalarProperty.set("a")
        s1.aggregate.nonNullableScalarProperty.set("b")

        doSynchronize()

        assertEquals("a", s0.aggregate.nonNullableScalarProperty.valueOrThrow)
        assertEquals("a", s1.aggregate.nonNullableScalarProperty.valueOrThrow)
    }

    @Test
    fun testList() {
        val s0Val = Clazz(1)
        val s1Val = Clazz(2)

        s0.list.add(s0Val)
        s1.list.add(s1Val)

        doSynchronize()

        assertEquals(listOf(1), s0.list.map {it.f})
        assertEquals(listOf(1), s1.list.map {it.f})

        assert(s0.list[0] === s0Val)
        assert(s1.list[0] !== s0Val)
    }

    @Test
    fun testDictSameKeys() {
        val firstValue = Clazz(1)
        s0.map[0] = firstValue
        s1.map[0] = Clazz(2)

        doSynchronize()

        assert(firstValue === s0.map[0]) { "Reference equality failed for s0 map" }
        assert(firstValue !== s1.map[0]) { "Reference equality failed for s1 map" }

        assertEquals(1, s0.map[0]!!.f)
        assertEquals(1, s1.map[0]!!.f)
    }

    @Test
    fun testDictDifferentKeys() {
        val firstValue = Clazz(1)
        val secondValue = Clazz(2)

        s0.map[0] = firstValue
        s1.map[1] = secondValue

        doSynchronize()

        assertEquals(1, s0.map[0]!!.f)
        assertEquals(1, s1.map[0]!!.f)

        assertEquals(2, s0.map[1]!!.f)
        assertEquals(2, s1.map[1]!!.f)

        assert(firstValue === s0.map[0]) { "Reference equality failed for s0 map" }
        assert(secondValue === s1.map[1]) { "Reference equality failed for s1 map" }
    }

    @Test
    fun testPerClientIdMap() {
        s0.protocol.contexts.getValueSet(SyncModelRoot.ClientId).addAll(listOf("A", "B", "C"))
        s1.protocol.contexts.getValueSet(SyncModelRoot.ClientId).addAll(listOf("C", "D", "E"))

        s0.property.set(Clazz(1))
        s1.property.set(Clazz(2))

        s0.property.valueOrThrow.mapPerClientIdPerContextMap["A"]!![1] = 1
        s0.property.valueOrThrow.mapPerClientIdPerContextMap["B"]!![1] = 1
        s0.property.valueOrThrow.mapPerClientIdPerContextMap["C"]!![1] = 1

        s1.property.valueOrThrow.mapPerClientIdPerContextMap["C"]!![1] = 2
        s1.property.valueOrThrow.mapPerClientIdPerContextMap["D"]!![1] = 2
        s1.property.valueOrThrow.mapPerClientIdPerContextMap["E"]!![1] = 2

        doSynchronize()

        // this behavior is consistent with top-level entity (Clazz) being fully replaced
        assertEquals(setOf("A", "B", "C"), s0.property.valueOrThrow.mapPerClientIdPerContextMap.map { it.component1() }.toSet())
        assertEquals(setOf("C", "D", "E"), s1.property.valueOrThrow.mapPerClientIdPerContextMap.map { it.component1() }.toSet())

        assertEquals(listOf(1 to 1), s0.property.valueOrThrow.mapPerClientIdPerContextMap["A"]!!.map { it.toPair() })
        assertEquals(listOf(1 to 1), s0.property.valueOrThrow.mapPerClientIdPerContextMap["B"]!!.map { it.toPair() })
        assertEquals(listOf(1 to 1), s0.property.valueOrThrow.mapPerClientIdPerContextMap["C"]!!.map { it.toPair() })

        assertEquals(listOf(1 to 1), s1.property.valueOrThrow.mapPerClientIdPerContextMap["C"]!!.map { it.toPair() })
        assertEquals(listOf<String>(), s1.property.valueOrThrow.mapPerClientIdPerContextMap["D"]!!.map { it.toPair() })
        assertEquals(listOf<String>(), s1.property.valueOrThrow.mapPerClientIdPerContextMap["E"]!!.map { it.toPair() })
    }

    @Test
    fun testProperty() {
        val s0Val = Clazz(1)
        val s1Val = Clazz(2)

        s0.property.set(s0Val)
        s1.property.set(s1Val)

        doSynchronize()

        assertEquals(s0Val.f, s0.property.valueOrThrow.f)
        assertEquals(s0Val.f, s1.property.valueOrThrow.f)

        assert(s0Val === s0.property.valueOrThrow)
        assert(s0Val !== s1.property.valueOrThrow)
    }

    @Test
    fun testDoNotSync() {
        s0.doNotSync.set(1)

        doSynchronize {! (it is RdBindableBase && it.location.localName == SyncModelRoot::doNotSync.name)}

        assertFalse { s1.doNotSync.hasValue }
    }

    @Test
    fun testExt() {
        s0.property.set(Clazz(1))
        s1.property.set(Clazz(2))

        s0.property.valueOrThrow.extToClazz.map[0] = Clazz(0)
        s1.property.valueOrThrow.extToClazz.map[1] = Clazz(1)

        val originalS0Ext = s0.property.valueOrThrow.extToClazz
        val originalS1Ext = s1.property.valueOrThrow.extToClazz

        doSynchronize()

        assert(originalS0Ext === s0.property.valueOrThrow.extToClazz)
        assert(originalS1Ext !== s1.property.valueOrThrow.extToClazz) // replacement of top-level entity

        assertEquals(listOf(0 to 0), s0.property.valueOrThrow.extToClazz.map.map { it.component1() to it.component2().f })
        assertEquals(listOf(0 to 0), s1.property.valueOrThrow.extToClazz.map.map { it.component1() to it.component2().f })
    }

}
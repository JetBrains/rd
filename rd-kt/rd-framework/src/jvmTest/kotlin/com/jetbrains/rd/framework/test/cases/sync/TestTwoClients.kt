package com.jetbrains.rd.framework.test.cases.sync

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.test.util.SequentialPumpingScheduler
import com.jetbrains.rd.framework.test.util.TestBase
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.reactive.hasValue
import com.jetbrains.rd.util.reactive.valueOrThrow
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import test.synchronization.Clazz
import test.synchronization.SyncModelRoot
import test.synchronization.extToClazz
import kotlin.assert
import kotlin.test.BeforeTest

class TestTwoClients : TestBase() {


    lateinit var c0: SyncModelRoot
    lateinit var c1: SyncModelRoot
    lateinit var s0: SyncModelRoot
    lateinit var s1: SyncModelRoot

    @BeforeTest
    fun setup() {
        ConsoleLoggerFactory.traceCategories.addAll(listOf("protocol", TestTwoClients::class.qualifiedName!!))

        val sc = SequentialPumpingScheduler

        val wireFactory = SocketWire.ServerFactory(lifetime, sc, null, false)
        val port = wireFactory.localPort

        val sp = mutableListOf<Protocol>()
        var spIdx = 0
        wireFactory.view(lifetime) { lf, wire ->
            val protocol = Protocol("s[${spIdx++}]", Serializers(), Identities(IdKind.Server), sc, wire, lf, initialContexts = *arrayOf<RdContext<*>>(SyncModelRoot.ClientId))
            sp.addUnique(lf, protocol)
        }


        var cpIdx = 0
        val cpFunc = { Protocol("c[${cpIdx++}]", Serializers(), Identities(IdKind.Client), sc, SocketWire.Client(lifetime, sc, port), lifetime, initialContexts = *arrayOf<RdContext<*>>(SyncModelRoot.ClientId)) }

        val cp = mutableListOf<Protocol>()
        cp.add(cpFunc())
        cp.add(cpFunc())

        wait { sp.size  == 2 }

        SyncModelRoot.ClientId.value = "Host"

        c0 = SyncModelRoot.create(lifetime, cp[0])
        c1 = SyncModelRoot.create(lifetime, cp[1])

        s0 = SyncModelRoot.create(lifetime, sp[0])
        s1 = SyncModelRoot.create(lifetime, sp[1])

        s0.synchronizeWith(lifetime, s1)
    }

    @After
    fun teardown() {
        ConsoleLoggerFactory.traceCategories.clear()
        SyncModelRoot.ClientId.value = null
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

    @Ignore("Enable after the branch with protocol contexts will be merged")
    @Test
    fun testPerClientIdMap() {
        c0.property.set(Clazz(1))
        wait { c1.property.hasValue }

        c0.property.valueOrThrow.mapPerClientId[1] = 1
        wait { c1.property.valueOrThrow.mapPerClientId[1] == 1 }
    }


    @Test
    fun testPerClientIdProperty() {
        c0.propPerClientId.set(1)
        wait { c1.propPerClientId.valueOrNull == 1 }
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
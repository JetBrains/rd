package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.RdBindableBase
import com.jetbrains.rider.framework.base.static
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.framework.impl.RdSignal
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.*
import org.testng.annotations.Test
import java.io.InputStream
import java.io.OutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFails

class RdSignalTest : RdTestBase() {
    @Test
    fun TestStatic() {
        val property_id = 1

        val client_property = RdSignal<Int>().static(property_id)
        val server_property = RdSignal<Int>().static(property_id)

        val clientLog = arrayListOf<Int>()
        val serverLog = arrayListOf<Int>()
        client_property.advise(Lifetime.Eternal, {clientLog.add(it)})
        server_property.advise(Lifetime.Eternal, {serverLog.add(it)})

        //not bound
        assertEquals(listOf<Int>(), clientLog)
        assertEquals(listOf<Int>(), serverLog)

        assertFails { client_property.fire(2) }
        assertFails { server_property.fire(2) }

        //bound
        clientProtocol.bindStatic(client_property, "top")
        serverProtocol.bindStatic(server_property, "top")

        //set from client
        client_property.fire(2)
        assertEquals(listOf(2), clientLog)
        assertEquals(listOf(2), serverLog)

        //set from server
        server_property.fire(3)
        assertEquals(listOf(2, 3), clientLog)
        assertEquals(listOf(2, 3), serverLog)
    }

    @Test
    fun TestDynamic() {
        val property_id = 1
        val client_property = RdProperty<DynamicEntity?>(null).static(property_id)
        val server_property = RdProperty<DynamicEntity?>(null).static(property_id).slave()

        DynamicEntity.create(clientProtocol)
        DynamicEntity.create(serverProtocol)
        //bound
        clientProtocol.bindStatic(client_property, "top")
        serverProtocol.bindStatic(server_property, "top")

        val clientLog = arrayListOf<String?>()
        val serverLog = arrayListOf<String?>()

        client_property.advise(Lifetime.Eternal, {entity -> entity?.foo?.advise(Lifetime.Eternal, { clientLog.add(it)})})
        server_property.advise(Lifetime.Eternal, {entity -> entity?.foo?.advise(Lifetime.Eternal, { serverLog.add(it)})})

        assertEquals(listOf<String?>(), clientLog)
        assertEquals(listOf<String?>(), serverLog)

        client_property.set(DynamicEntity())

        assertEquals(listOf<String?>(), clientLog)
        assertEquals(listOf<String?>(), serverLog)

        client_property.value?.foo?.fire("F")
        assertEquals(listOf<String?>("F"), clientLog)
        assertEquals(listOf<String?>("F"), serverLog)

        server_property.value?.foo?.fire("")
        assertEquals(listOf<String?>("F", ""), clientLog)
        assertEquals(listOf<String?>("F", ""), serverLog)

        server_property.value?.foo?.fire(null)
        assertEquals(listOf("F", "", null), clientLog)
        assertEquals(listOf("F", "", null), serverLog)

        client_property.set(DynamicEntity()) //no listen - no change
        assertEquals(listOf("F", "", null), clientLog)
        assertEquals(listOf("F", "", null), serverLog)

        server_property.value?.foo?.fire(null)
        assertEquals(listOf("F", "", null, null), clientLog)
        assertEquals(listOf("F", "", null, null), serverLog)
    }

    @Suppress("UNCHECKED_CAST")
    private class DynamicEntity(val _foo: RdSignal<String?>) : RdBindableBase() {
        val foo : ISignal<String?> = _foo

        companion object : IMarshaller<DynamicEntity> {
            override fun read(ctx: SerializationCtx, stream: InputStream): DynamicEntity {
                return DynamicEntity(RdSignal.read(ctx, stream, FrameworkMarshallers.String.nullable()))
            }

            override fun write(ctx: SerializationCtx, stream: OutputStream, value: DynamicEntity) {
                RdSignal.write(ctx, stream, value._foo)
            }

            override val _type: Class<*>
                get() = DynamicEntity::class.java

            fun create(protocol: IProtocol) {
                protocol.serializers.register(DynamicEntity)
            }
        }

        override fun init(lifetime: Lifetime) {
            _foo.bind(lifetime, this, "foo")
        }

        override fun identify(ids: IIdentities) {
            _foo.identify(ids)
        }


        constructor() : this(RdSignal<String?>(FrameworkMarshallers.String.nullable()))
    }

    @Test
    fun TestVoidStatic() {
        val client_signal = clientProtocol.bindStatic(RdSignal<Unit>().static(1), "top")
        val server_signal = serverProtocol.bindStatic(RdSignal<Unit>().static(1), "top")

        var acc = 0
        Lifetime.using {
            server_signal.advise(it, {acc++})
            assertEquals(0, acc)

            client_signal.fire()
            assertEquals(1, acc)

            client_signal.fire()
            assertEquals(2, acc)
        }

        client_signal.fire() //no transmitting
        assertEquals(2, acc)
    }


    @Test
    fun TestAsyncSignalStatic() {
        val client_signal = clientProtocol.bindStatic(RdSignal<Unit>().static(1), "top")
        val server_signal = serverProtocol.bindStatic(RdSignal<Unit>().static(1), "top")

        var acc = 0
        Lifetime.using { lf ->
            server_signal.adviseOn(lf, serverBgScheduler, {
                serverBgScheduler.assertThread()
                Thread.sleep(100)
                acc++
            })

            assertFails {server_signal.advise(lf, {}) }

            assertEquals(0, acc)

            client_signal.fire()

            assertEquals(0, acc) //no change

            client_signal.fire()
            assertEquals(0, acc) //still no change

            serverBgScheduler.flushScheduler()
            assertEquals(2, acc)
        }

        client_signal.fire() //no transmitting
        serverBgScheduler.assertNoExceptions()
        assertEquals(2, acc)
    }

    @Test
    fun TestVoidDynamic() {
        val client_property = clientProtocol.bindStatic(RdProperty<VoidSignalEntity?>(null).static(1), "top")
        val server_property = serverProtocol.bindStatic(RdProperty<VoidSignalEntity?>(null).slave().static(1), "top")

        clientProtocol.serializers.register(VoidSignalEntity)
        serverProtocol.serializers.register(VoidSignalEntity)

        var acc = 0
        client_property.adviseNotNull(Lifetime.Eternal, {it.foo.fire()})
        server_property.viewNotNull(Lifetime.Eternal,
                {lf, entity -> entity.foo.advise(lf,
                    {
                        acc++
                        lf.add { acc-- }
                    })
                }
        )

        assertEquals(0, acc)
        client_property.value = VoidSignalEntity()
        assertEquals(1, acc)
        client_property.value = null
        assertEquals(0, acc)
    }

    private class VoidSignalEntity(private val _foo: RdSignal<Unit>) : RdBindableBase() {
        val foo : RdSignal<Unit> get() = _foo

        companion object : IMarshaller<VoidSignalEntity> {
            override fun read(ctx: SerializationCtx, stream: InputStream): VoidSignalEntity {
                val foo = RdSignal.read(ctx, stream, FrameworkMarshallers.Void)
                return VoidSignalEntity(foo)
            }

            override fun write(ctx: SerializationCtx, stream: OutputStream, value: VoidSignalEntity) {
                RdSignal.write(ctx, stream, value._foo)
            }

            override val _type: Class<*>
                get() = VoidSignalEntity::class.java

        }

        override fun init(lifetime: Lifetime) {
            _foo.bind(lifetime, this, "foo")
        }

        override fun identify(ids: IIdentities) {
            _foo.identify(ids)
        }

        constructor(): this(RdSignal<Unit>())
    }
}



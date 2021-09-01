package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.RdBindableBase
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdProperty
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.onTermination
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.threading.SynchronousScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass


class RdSignalTest : RdFrameworkTestBase() {
    @Test
    fun TestStatic() {
        val property_id = 1

        val client_property = RdSignal<Int>().static(property_id)
        val server_property = RdSignal<Int>().static(property_id)

        val clientLog = ArrayList<Int>()
        val serverLog = ArrayList<Int>()
        client_property.advise(Lifetime.Eternal) { clientLog.add(it) }
        server_property.advise(Lifetime.Eternal) { serverLog.add(it) }

        //not bound
        assertEquals(listOf<Int>(), clientLog)
        assertEquals(listOf<Int>(), serverLog)

//        assertFails { client_property.fire(2) }
//        assertFails { server_property.fire(2) }

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

        val clientLog = ArrayList<String?>()
        val serverLog = ArrayList<String?>()

        client_property.advise(Lifetime.Eternal) { entity -> entity?.foo?.advise(Lifetime.Eternal, { clientLog.add(it) }) }
        server_property.advise(Lifetime.Eternal) { entity -> entity?.foo?.advise(Lifetime.Eternal, { serverLog.add(it) }) }

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
        override fun deepClone(): IRdBindable {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        val foo: ISignal<String?> = _foo

        companion object : IMarshaller<DynamicEntity> {
            override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DynamicEntity {
                return DynamicEntity(RdSignal.read(ctx, buffer, FrameworkMarshallers.String.nullable()))
            }

            override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DynamicEntity) {
                RdSignal.write(ctx, buffer, value._foo)
            }

            override val _type: KClass<*>
                get() = DynamicEntity::class

            fun create(protocol: IProtocol) {
                protocol.serializers.register(DynamicEntity)
            }
        }

        override fun init(lifetime: Lifetime) {
            _foo.bind(lifetime, this, "foo")
        }

        override fun identify(identities: IIdentities, id: RdId) {
            _foo.identify(identities, id.mix("foo"))
        }


        constructor() : this(RdSignal<String?>(FrameworkMarshallers.String.nullable()))
    }

    @Test
    fun TestVoidStatic() {
        val client_signal = clientProtocol.bindStatic(RdSignal<Unit>().static(1), "top")
        val server_signal = serverProtocol.bindStatic(RdSignal<Unit>().static(1), "top")

        var acc = 0
        Lifetime.using { lifetime ->
            server_signal.advise(lifetime) { acc++ }
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
    fun TestVoidDynamic() {
        val client_property = clientProtocol.bindStatic(RdProperty<VoidSignalEntity?>(null).static(1), "top")
        val server_property = serverProtocol.bindStatic(RdProperty<VoidSignalEntity?>(null).slave().static(1), "top")

        clientProtocol.serializers.register(VoidSignalEntity)
        serverProtocol.serializers.register(VoidSignalEntity)

        var acc = 0
        client_property.adviseNotNull(Lifetime.Eternal) { it.foo.fire() }
        server_property.viewNotNull(Lifetime.Eternal) { lf, entity ->
            entity.foo.advise(lf) {
                acc++
                lf.onTermination { acc-- }
            }
        }

        assertEquals(0, acc)
        client_property.value = VoidSignalEntity()
        assertEquals(1, acc)
        client_property.value = null
        assertEquals(0, acc)
    }

    data class Foo(val negate: Boolean = false, val module: Int = 0)

    class CustomSerializer : ISerializer<Foo> {
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Foo {
            val negate = buffer.readBoolean()
            val module = buffer.readInt()
            return Foo(negate, module)
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Foo) {
            buffer.writeBoolean(value.negate)
            buffer.writeInt(value.module)
        }
    }

    @Test
    fun TestCustomSerializer() {
        val property_id = 1

        val client_property = RdSignal<Foo>(CustomSerializer()).static(property_id)
        val server_property = RdSignal<Foo>(CustomSerializer()).static(property_id)

        var clientLog = Foo()
        var serverLog = Foo()
        client_property.advise(Lifetime.Eternal) { clientLog = it }
        server_property.advise(Lifetime.Eternal) { serverLog = it }

        //bound
        clientProtocol.bindStatic(client_property, "top")
        serverProtocol.bindStatic(server_property, "top")

        //set from client
        client_property.fire(Foo(true, 2))
        assertEquals(Foo(true, 2), clientLog)
        assertEquals(Foo(true, 2), serverLog)

        //set from server
        server_property.fire(Foo(false, 3))
        assertEquals(Foo(false, 3), clientLog)
        assertEquals(Foo(false, 3), serverLog)
    }

    @Test
    fun testAssignedSchedulerAfterBind() {
        require(clientProtocol.scheduler !== SynchronousScheduler) { "Test assumes protocol default scheduler is not Sync" }

        val signal = RdSignal<Unit>()
        signal.wireScheduler = SynchronousScheduler
        clientProtocol.bindStatic(signal.static(1), "top")

        assertEquals(SynchronousScheduler, signal.wireScheduler)
    }

    // this test can be removed together with the deprecated method it tests
    @Test
    fun testAdvisedSchedulerAfterBind() {
        require(clientProtocol.scheduler !== SynchronousScheduler) { "Test assumes protocol default scheduler is not Sync" }

        val signal = RdSignal<Unit>()
        signal.adviseOn(Lifetime.Eternal, SynchronousScheduler) { _ -> }
        clientProtocol.bindStatic(signal.static(1), "top")

        assertEquals(SynchronousScheduler, signal.wireScheduler)
    }

    private class VoidSignalEntity(private val _foo: RdSignal<Unit>) : RdBindableBase() {
        override fun deepClone(): IRdBindable {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        val foo: RdSignal<Unit> get() = _foo

        companion object : IMarshaller<VoidSignalEntity> {
            override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): VoidSignalEntity {
                val foo = RdSignal.read(ctx, buffer, FrameworkMarshallers.Void)
                return VoidSignalEntity(foo)
            }

            override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: VoidSignalEntity) {
                RdSignal.write(ctx, buffer, value._foo)
            }

            override val _type: KClass<*>
                get() = VoidSignalEntity::class

        }

        override fun init(lifetime: Lifetime) {
            _foo.bind(lifetime, this, "foo")
        }

        override fun identify(identities: IIdentities, id: RdId) {
            _foo.identify(identities, id.mix("foo"))
        }

        constructor() : this(RdSignal<Unit>())
    }
}



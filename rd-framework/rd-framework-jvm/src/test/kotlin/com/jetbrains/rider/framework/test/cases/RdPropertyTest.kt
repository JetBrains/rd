package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.AbstractBuffer
import com.jetbrains.rider.framework.base.RdBindableBase
import com.jetbrains.rider.util.string.printToString
import com.jetbrains.rider.framework.base.static
import com.jetbrains.rider.framework.impl.RdOptionalProperty
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IProperty
import com.jetbrains.rider.util.reactive.valueOrThrow
import org.testng.annotations.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class RdPropertyTest : RdTestBase() {
    @Test
    fun TestStatic() {
        val property_id = 1

        val client_property = RdProperty(1).static(property_id)
        val server_property = RdProperty(1).static(property_id).slave()

        val clientLog = arrayListOf<Int>()
        val serverLog = arrayListOf<Int>()
        client_property.advise(Lifetime.Eternal, {clientLog.add(it)})
        server_property.advise(Lifetime.Eternal, {serverLog.add(it)})

        //not bound
        assertEquals(listOf(1), clientLog)
//        assertEquals(listOf(1), serverLog)

//        assertFails { client_property.value = 2 } do not count NotBound any more
//        assertFails { server_property.value = 2 }
        assertEquals(listOf(1), clientLog)
//        assertEquals(listOf(1), serverLog)

        //bound
        serverProtocol.bindStatic(server_property, "top")
        clientProtocol.bindStatic(client_property, "top")

        assertEquals(listOf(1), clientLog)
        assertEquals(listOf(1), serverLog)

        //set from client
        client_property.value = 2
        assertEquals(listOf(1, 2), clientLog)
        assertEquals(listOf(1, 2), serverLog)

        //set from server
        server_property.value = 3
        assertEquals(listOf(1, 2, 3), clientLog)
        assertEquals(listOf(1, 2, 3), serverLog)
    }


    @Test
    fun TestDynamic() {
        val property_id = 1
        val client_property = RdOptionalProperty<DynamicEntity>().static(property_id)
        val server_property = RdOptionalProperty<DynamicEntity>().static(property_id).slave()

        DynamicEntity.create(clientProtocol)
        DynamicEntity.create(serverProtocol)
        //bound
        clientProtocol.bindStatic(client_property, "top")
        serverProtocol.bindStatic(server_property, "top")

        val clientLog = arrayListOf<Boolean?>()
        val serverLog = arrayListOf<Boolean?>()

        client_property.advise(Lifetime.Eternal, {entity -> entity.foo.advise(Lifetime.Eternal, { clientLog.add(it)})})
        server_property.advise(Lifetime.Eternal, {entity -> entity.foo.advise(Lifetime.Eternal, { serverLog.add(it)})})

        assertEquals(listOf<Boolean?>(), clientLog)
        assertEquals(listOf<Boolean?>(), serverLog)

        client_property.set(DynamicEntity(null))

        assertEquals(listOf<Boolean?>(null), clientLog)
        assertEquals(listOf<Boolean?>(null), serverLog)


        client_property.valueOrThrow.foo.set(false)
        assertNotNull(client_property.printToString())
        assertEquals(listOf(null, false), clientLog)
        assertEquals(listOf(null, false), serverLog)

        server_property.valueOrThrow.foo.set(true)
        assertEquals(listOf(null, false, true), clientLog)
        assertEquals(listOf(null, false, true), serverLog)

        server_property.valueOrThrow.foo.set(null)
        assertEquals(listOf(null, false, true, null), clientLog)
        assertEquals(listOf(null, false, true, null), serverLog)


        val e = DynamicEntity(true)
        client_property.set(e) //no listen - no change
        assertEquals(listOf(null, false, true, null, true), clientLog)
        assertEquals(listOf(null, false, true, null, true), serverLog)

        server_property.valueOrThrow.foo.set(null)
        assertEquals(listOf(null, false, true, null, true, null), clientLog)
        assertEquals(listOf(null, false, true, null, true, null), serverLog)


        client_property.set(DynamicEntity(false))
        //reuse
        client_property.set(e)

    }

    private class DynamicEntity(val _foo: RdProperty<Boolean?>) : RdBindableBase() {
        val foo : IProperty<Boolean?> = _foo

        companion object : IMarshaller<DynamicEntity> {
            override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DynamicEntity {
                return DynamicEntity(RdProperty.read(ctx, buffer, FrameworkMarshallers.Bool.nullable()))
            }

            override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DynamicEntity) {
                RdProperty.write(ctx, buffer, value._foo)
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

        override fun identify(identities: IIdentities, ids: RdId) {
            _foo.identify(identities, ids.mix("foo"))
        }

        constructor(_foo : Boolean?) : this(RdProperty(_foo, FrameworkMarshallers.Bool.nullable()))
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun TestEarlyAdvise() {
        setWireAutoFlush(false)

        val szr = RdOptionalProperty.Companion as ISerializer<RdOptionalProperty<Int>>
        val p1 =  RdOptionalProperty(szr)
        val p2 =  RdOptionalProperty(szr)

        var nxt = 0
        val log = arrayListOf<Int>()
        p1.view(clientLifetimeDef.lifetime, {lf, inner -> inner.advise(lf) {log.add(it)}})
        p2.advise(serverLifetimeDef.lifetime, { inner -> inner.set(++nxt)  })

        clientProtocol.bindStatic(p1, 1)
        serverProtocol.bindStatic(p2, 1)
        p1.set(RdOptionalProperty<Int>())

        setWireAutoFlush(true)
        assertEquals(listOf(1), log)



    }
}



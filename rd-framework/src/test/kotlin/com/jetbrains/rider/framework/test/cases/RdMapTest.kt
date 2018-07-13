package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.RdBindableBase
import com.jetbrains.rider.framework.base.static
import com.jetbrains.rider.framework.impl.RdMap
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IProperty
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RdMapTest : RdFrameworkTestBase() {
    @Test
    fun testStatic()
    {
        val id = 1

        val serverMap = RdMap<Int, String>().static(id).apply { optimizeNested=true }
        val clientMap = RdMap<Int, String>().static(id).apply { optimizeNested=true }

        val logUpdate = arrayListOf<String>()
        clientMap.advise(Lifetime.Eternal) { entry -> logUpdate.add(entry.toString())}

        assertEquals(0, serverMap.count())
        assertEquals(0, clientMap.count())

        serverMap[1] = "Server value 1"
        serverMap[2] = "Server value 2"
        serverMap[3] = "Server value 3"

        assertEquals(0, clientMap.count())
        clientProtocol.bindStatic(clientMap, "top")
        serverProtocol.bindStatic(serverMap, "top")

        assertEquals(3, clientMap.count())
        assertEquals("Server value 1", clientMap[1])
        assertEquals("Server value 2", clientMap[2])
        assertEquals("Server value 3", clientMap[3])

        serverMap[4] = "Server value 4"
        clientMap[4] = "Client value 4"

        assertEquals("Client value 4", clientMap[4])
        assertEquals("Client value 4", serverMap[4])

        clientMap[5] = "Client value 5"
        serverMap[5] = "Server value 5"


        assertEquals("Server value 5", clientMap[5])
        assertEquals("Server value 5", serverMap[5])


        assertEquals(listOf("Add 1:Server value 1",
                "Add 2:Server value 2",
                "Add 3:Server value 3",
                "Add 4:Server value 4",
                "Update 4:Client value 4",
                "Add 5:Client value 5",
                "Update 5:Server value 5"),
            logUpdate
        )
    }

    @Test
    fun testDynamic()
    {
        val id = 1

        val serverMap = RdMap<Int, DynamicEntity>().static(id)
        val clientMap = RdMap<Int, DynamicEntity>().static(id)
        serverMap.manualMaster = false

        DynamicEntity.create(clientProtocol)
        DynamicEntity.create(serverProtocol)

        assertTrue(serverMap.count() == 0)
        assertTrue(clientMap.count() == 0)

        clientProtocol.bindStatic(clientMap, "top")
        serverProtocol.bindStatic(serverMap," top")

        val log = arrayListOf<String>()
        serverMap.view(Lifetime.Eternal, {lf, k, v ->
            lf.bracket({log.add("start $k")}, {log.add("finish $k")})
            v.foo.advise(lf, {fooval -> log.add("$fooval")})
        })
        clientMap[1] = DynamicEntity(null)
        clientMap[1]!!.foo.value = true
        clientMap[1]!!.foo.value = true //no action

        clientMap[1] = DynamicEntity(true)

        serverMap[2] = DynamicEntity(false)

        clientMap.remove(2)
        clientMap[2] = DynamicEntity(true)

        clientMap.clear()

        assertEquals(listOf("start 1", "null", "true",
                "finish 1", "start 1", "true",
                "start 2", "false",
                "finish 2", "start 2", "true",
                "finish 1", "finish 2"), log)
    }


    @Suppress("UNCHECKED_CAST")
    private class DynamicEntity(val _foo: RdProperty<Boolean?>) : RdBindableBase() {
        val foo : IProperty<Boolean?> = _foo

        companion object : IMarshaller<DynamicEntity> {
            override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DynamicEntity {
                return DynamicEntity(
                    RdProperty.read(
                        ctx,
                        buffer
                    ) as RdProperty<Boolean?>
                )
            }

            override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DynamicEntity) {
                RdProperty.write(ctx, buffer, value.foo as RdProperty<Boolean?>)
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

        constructor(_foo : Boolean?) : this(RdProperty(_foo))
    }
}
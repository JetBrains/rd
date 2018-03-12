package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.AbstractBuffer
import com.jetbrains.rider.framework.base.RdBindableBase
import com.jetbrains.rider.framework.base.static
import com.jetbrains.rider.framework.impl.RdList
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IProperty
import org.testng.Assert
import org.testng.annotations.Test

class RdListTest : RdTestBase() {
    @Test
    fun TestStatic()
    {
        val id = 1

        val serverList = RdList<String>().static(id).apply { optimizeNested=true }
        val clientList = RdList<String>().static(id).apply { optimizeNested=true }

        val logUpdate = arrayListOf<String>()
        clientList.advise(Lifetime.Eternal) { entry -> logUpdate.add("${entry.javaClass.simpleName} ${entry.index}:${entry.newValueOpt}")}

        Assert.assertTrue(serverList.count() == 0)
        Assert.assertTrue(clientList.count() == 0)

        serverList.add("Server value 1")
        serverList.addAll(listOf("Server value 2", "Server value 3"))

        Assert.assertEquals(0, clientList.count())
        clientProtocol.bindStatic(clientList, "top")
        serverProtocol.bindStatic(serverList, "top")

        Assert.assertEquals(clientList.count(), 3)
        Assert.assertEquals(clientList[0], "Server value 1")
        Assert.assertEquals(clientList[1], "Server value 2")
        Assert.assertEquals(clientList[2], "Server value 3")

        serverList.add("Server value 4")
        clientList[3] = "Client value 4"

        Assert.assertEquals(clientList[3], "Client value 4")
        Assert.assertEquals(serverList[3], "Client value 4")

        clientList.add("Client value 5")
        serverList[4] = "Server value 5"


        Assert.assertEquals(clientList[4], "Server value 5")
        Assert.assertEquals(serverList[4], "Server value 5")


        Assert.assertEquals(logUpdate,
            listOf("Add 0:Server value 1",
            "Add 1:Server value 2",
            "Add 2:Server value 3",
            "Add 3:Server value 4",
            "Update 3:Client value 4",
            "Add 4:Client value 5",
            "Update 4:Server value 5")
        )
    }

    @Test
    fun TestDynamic()
    {
        val id = 1

        val serverList = RdList<DynamicEntity>().static(id)
        val clientList = RdList<DynamicEntity>().static(id)

        DynamicEntity.create(clientProtocol)
        DynamicEntity.create(serverProtocol)

        Assert.assertTrue(serverList.count() == 0)
        Assert.assertTrue(clientList.count() == 0)

        clientProtocol.bindStatic(clientList, "top")
        serverProtocol.bindStatic(serverList," top")

        val log = arrayListOf<String>()
        serverList.view(Lifetime.Eternal, { lf, k, v ->
            lf.bracket({log.add("start $k")}, {log.add("finish $k")})
            v.foo.advise(lf, {fooval -> log.add("$fooval")})
        })
        clientList.add(DynamicEntity(null))
        clientList[0].foo.value = true
        clientList[0].foo.value = true //no action

        clientList[0] = DynamicEntity(true)

        serverList.add(DynamicEntity(false))

        clientList.removeAt(1)
        clientList.add(DynamicEntity(true))

        clientList.clear()

        Assert.assertEquals(log, listOf("start 0", "null", "true",
            "finish 0", "start 0", "true",
            "start 1", "false",
            "finish 1", "start 1", "true",
            "finish 1", "finish 0"))
    }


    @Suppress("UNCHECKED_CAST")
    private class DynamicEntity(val _foo: RdProperty<Boolean?>) : RdBindableBase() {
        val foo : IProperty<Boolean?> = _foo

        companion object : IMarshaller<DynamicEntity> {
            override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DynamicEntity {
                return DynamicEntity(RdProperty.read(ctx, buffer) as RdProperty<Boolean?>)
            }

            override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DynamicEntity) {
                RdProperty.write(ctx, buffer, value.foo as RdProperty<Boolean?>)
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

        override fun identify(identities: IIdentities, id: RdId) {
            _foo.identify(identities, id.mix("foo"))
        }

        constructor(_foo : Boolean?) : this(RdProperty(_foo))
    }
}
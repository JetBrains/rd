package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.*
import com.jetbrains.rider.framework.impl.RdOptionalProperty
import com.jetbrains.rider.framework.impl.RdProperty
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.IProperty
import com.jetbrains.rider.util.reactive.valueOrThrow
import org.testng.annotations.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals

class RdExtTest : RdTestBase() {
    @Test(enabled = false) // TODO: RIDER-14180
    fun testExtension() {
        val propertyId = 1
        val clientProperty = RdOptionalProperty<DynamicEntity>().static(propertyId)
        val serverProperty = RdOptionalProperty<DynamicEntity>().static(propertyId).slave()

        DynamicEntity.create(clientProtocol)
        DynamicEntity.create(serverProtocol)
        //bound
        clientProtocol.bindStatic(clientProperty, "top")
        serverProtocol.bindStatic(serverProperty, "top")


        clientWire.autoFlush = false
        serverWire.autoFlush = false

        var clientEntity = DynamicEntity(true)
        clientProperty.set(clientEntity)
        clientWire.processAllMessages()

        val serverEntity = DynamicEntity(true)
        serverProperty.set(serverEntity)
        serverWire.processAllMessages()

        //it's new!
        clientEntity = clientProperty.valueOrThrow

        clientEntity.getOrCreateExtension("ext", DynamicExt::class) { DynamicExt("Ext!", "client") }
        clientWire.processAllMessages()
        //client send READY

        val serverExt = serverEntity.getOrCreateExtension("ext", DynamicExt::class) { DynamicExt("", "server") }
        serverWire.processAllMessages()
        //server send READY


        clientWire.processAllMessages()
        //client send COUNTERPART_ACK

        serverWire.processAllMessages()
        //server send COUNTERPART_ACK

        assertEquals("Ext!", serverExt.bar.value)
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

    private class DynamicExt(val _bar: RdProperty<String>, private val debugName: String) : RdExtBase(), ISerializersOwner {
        override fun registerSerializersCore(serializers: ISerializers) {}

        override val serializersOwner: ISerializersOwner get() = this

        val bar: IProperty<String> = _bar

        init {
            bindableChildren.add("bar" to _bar)
            _bar.slave()
        }

        companion object : IMarshaller<DynamicExt> {
            override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DynamicExt {
                throw IllegalStateException()
            }

            override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DynamicExt) {
                RdProperty.write(ctx, buffer, value._bar)
            }

            override val _type: KClass<*>
                get() = DynamicExt::class

            fun create(protocol: IProtocol) {
                protocol.serializers.register(DynamicExt)
            }
        }

        constructor(_bar: String, debugName: String) : this(RdProperty(_bar, FrameworkMarshallers.String), debugName)
    }

}
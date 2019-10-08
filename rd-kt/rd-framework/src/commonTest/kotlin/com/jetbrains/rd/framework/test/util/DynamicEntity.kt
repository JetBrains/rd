package com.jetbrains.rd.framework.test.util

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.RdBindableBase
import com.jetbrains.rd.framework.impl.RdProperty
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IProperty
import kotlin.reflect.KClass


class DynamicEntity<T>(val _foo: RdProperty<T>) : RdBindableBase() {
    override fun deepClone(): IRdBindable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    val foo: IProperty<T> = _foo

    companion object : IMarshaller<DynamicEntity<*> > {
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DynamicEntity<*> {
            return DynamicEntity(RdProperty.read(ctx, buffer))
        }

        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DynamicEntity<*>) {
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

    override fun identify(identities: IIdentities, id: RdId) {
        _foo.identify(identities, id.mix("foo"))
    }

    constructor(_foo: T) : this(RdProperty<T>(_foo, Polymorphic<T>()))
}
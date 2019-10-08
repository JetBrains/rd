package com.jetbrains.rd.framework.test.cases.interning

import com.jetbrains.rd.framework.IIdentities
import com.jetbrains.rd.framework.RdId
import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.base.IRdBindable
import com.jetbrains.rd.framework.base.RdBindableBase
import com.jetbrains.rd.framework.impl.RdOptionalProperty
import com.jetbrains.rd.util.lifetime.Lifetime


class PropertyHolderWithInternRoot<T : Any>(val property: RdOptionalProperty<T>, var mySerializationContext: SerializationCtx) : RdBindableBase() {
    override fun deepClone(): IRdBindable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun init(lifetime: Lifetime) {
        property.bind(lifetime, this, "propertyHolderWithInternRoot")
        super.init(lifetime)
    }

    override fun identify(identities: IIdentities, id: RdId) {
        property.identify(identities, id.mix("propertyHolderWithInternRoot"))
        super.identify(identities, id)
    }



    override val serializationContext: SerializationCtx
        get() = mySerializationContext
}
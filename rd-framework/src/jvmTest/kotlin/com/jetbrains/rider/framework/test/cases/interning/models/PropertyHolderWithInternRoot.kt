package com.jetbrains.rider.framework.test.cases.interning

import com.jetbrains.rider.framework.IIdentities
import com.jetbrains.rider.framework.RdId
import com.jetbrains.rider.framework.SerializationCtx
import com.jetbrains.rider.framework.base.RdBindableBase
import com.jetbrains.rider.framework.impl.RdOptionalProperty
import com.jetbrains.rider.util.lifetime.Lifetime


class PropertyHolderWithInternRoot<T : Any>(val property: RdOptionalProperty<T>, var mySerializationContext: SerializationCtx) : RdBindableBase() {
    override fun init(lifetime: Lifetime) {
        property.bind(lifetime, this, "propertyHolderWithInternRoot")
        super.init(lifetime)
    }

    override fun identify(identities: IIdentities, ids: RdId) {
        property.identify(identities, ids.mix("propertyHolderWithInternRoot"))
        super.identify(identities, ids)
    }



    override val serializationContext: SerializationCtx
        get() = mySerializationContext
}
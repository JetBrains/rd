package com.jetbrains.rider.rdtext.impl.intrinsics

import com.jetbrains.rider.framework.FrameworkMarshallers

enum class RdChangeOrigin {
    Slave,
    Master;

    companion object { val marshaller = FrameworkMarshallers.enum<RdChangeOrigin>() }
}
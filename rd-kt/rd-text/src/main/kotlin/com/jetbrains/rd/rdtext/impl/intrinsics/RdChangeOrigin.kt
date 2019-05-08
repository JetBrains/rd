package com.jetbrains.rd.rdtext.impl.intrinsics

import com.jetbrains.rd.framework.FrameworkMarshallers

enum class RdChangeOrigin {
    Slave,
    Master;

    companion object { val marshaller = FrameworkMarshallers.enum<RdChangeOrigin>() }
}
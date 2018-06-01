package com.jetbrains.rider.rdtext.impl.ot

import com.jetbrains.rider.rdtext.RdChangeOrigin

data class RdAck(val timestamp: Int, val origin: RdChangeOrigin)
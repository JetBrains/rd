package com.jetbrains.rider.framework.base

abstract class RdDelegateBase<out T : IRdReactive>(val delegate: T) : IRdReactive by delegate

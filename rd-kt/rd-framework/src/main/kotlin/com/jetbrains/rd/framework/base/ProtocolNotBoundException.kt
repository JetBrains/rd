package com.jetbrains.rd.framework.base

class ProtocolNotBoundException(id: String) : Exception("$id is not bound to a protocol")
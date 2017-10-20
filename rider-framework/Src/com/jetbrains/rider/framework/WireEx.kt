package com.jetbrains.rider.framework

val IWire.serverPort: Int get() {
    val serverSocketWire = this as? SocketWire.Server ?: throw IllegalArgumentException("You must use SocketWire.Server to get server port")
    return serverSocketWire.port
}

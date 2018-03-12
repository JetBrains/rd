package com.jetbrains.rider.util.reactive

import com.jetbrains.rider.util.getThrowableText
import java.util.concurrent.ExecutionException

class RdFault constructor(val reasonTypeFqn: String, val reasonMessage: String, val reasonAsText: String, reason: Throwable? = null)
    : ExecutionException(reasonMessage + if (reason == null)  ", reason: $reasonAsText" else "", reason) {

    constructor (reason: Throwable) : this (reason.javaClass.name, reason.message ?: "-- no message --", reason.getThrowableText(), reason) {
    }
}
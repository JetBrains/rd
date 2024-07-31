package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.getThrowableText
import java.util.concurrent.ExecutionException


class RdFault constructor(val reasonTypeFqn: String, val reasonMessage: String, val reasonAsText: String, reason: Throwable? = null)
    : ExecutionException(reasonMessage + if (reason == null)  ", reason: $reasonAsText" else "", reason) {

    constructor (reason: Throwable) : this (reason::class.simpleName?:"", reason.message ?: "-- no message --", reason.getThrowableText(), reason) {
    }
}
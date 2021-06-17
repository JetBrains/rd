package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.ExecutionException

expect class RdFault constructor(reasonTypeFqn: String, reasonMessage: String, reasonAsText: String, reason: Throwable? = null) :
    ExecutionException {
    val reasonTypeFqn: String
    val reasonMessage: String
    val reasonAsText: String

    constructor(reason: Throwable)
}
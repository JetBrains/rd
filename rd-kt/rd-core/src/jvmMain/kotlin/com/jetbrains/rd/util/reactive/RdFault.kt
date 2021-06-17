package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.ExecutionException
import com.jetbrains.rd.util.getThrowableText
import org.jetbrains.annotations.Nls

actual class RdFault actual constructor(
    actual val reasonTypeFqn: String,
    @Nls actual val reasonMessage: String,
    @Nls actual val reasonAsText: String,
    reason: Throwable?
) : ExecutionException(reasonMessage + if (reason == null) ", reason: $reasonAsText" else "", reason) {

    actual constructor (reason: Throwable) : this(
        reason::class.simpleName ?: "",
        reason.message ?: "-- no message --",
        reason.getThrowableText(),
        reason
    )
}
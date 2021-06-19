package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.ExecutionException
import com.jetbrains.rd.util.getThrowableText
import org.jetbrains.annotations.Nls

actual class RdFault actual constructor(
    actual val reasonTypeFqn: String,
    actual val reasonMessage: String,
    @field:Nls @get:Nls @Nls actual val localizedReasonMessage: String,
    actual val reasonAsText: String,
    reason: Throwable?
) : ExecutionException(reasonMessage + if (reason == null) ", reason: $reasonAsText" else "", reason) {

    actual constructor(reason: Throwable) : this(
        reason::class.simpleName ?: "",
        reason.message ?: "-- no message --",
        reason.localizedMessage ?: reason.message ?: "-- no message --",
        reason.getThrowableText(),
        reason
    )

    @Deprecated("Use the override with localizedReasonMessage")
    actual constructor(reasonTypeFqn: String, reasonMessage: String, reasonAsText: String, reason: Throwable?) : this(
        reasonTypeFqn,
        reasonMessage,
        reasonMessage,
        reasonAsText,
        reason
    )
}

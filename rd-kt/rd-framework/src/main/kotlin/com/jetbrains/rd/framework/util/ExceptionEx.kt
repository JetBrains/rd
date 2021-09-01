package com.jetbrains.rd.framework.util

import java.util.concurrent.CompletionException

internal fun CompletionException.unwrap(): Throwable {
    var e: Throwable = this
    while (true) e = e.cause as? CompletionException ?: return e
}
package com.jetbrains.rd.cross.util

import java.io.Closeable
import java.time.LocalTime

fun logWithTime(message: String) {
    println("At ${LocalTime.now()}: $message ")
}

class LoggingCookie(private val action: String) : Closeable {
    init {
        logWithTime("$action started")
    }

    override fun close() {
        logWithTime("$action finished")
    }
}

inline fun <T> trackAction(message: String, action: () -> T): T {
    LoggingCookie(message).use {
        return action()
    }
}
package com.jetbrains.rd.cross.util

import com.jetbrains.rd.framework.base.RdExtBase
import com.jetbrains.rd.util.*
import java.io.Closeable
import java.time.LocalTime
import kotlin.io.use

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

class CrossTestsLoggerFactory(val buffer: MutableList<String>) : ILoggerFactory {

    companion object {
        private val includedCategories = arrayOf("protocol.SEND", "protocol.RECV")
        private val excludedRegex = Regex(RdExtBase.ExtState.values().joinToString(separator = "|", transform = RdExtBase.ExtState::toString))
    }

    fun logFormat(category: String, level: LogLevel, message: Any?, throwable: Throwable?): String {
        val throwableToPrint = if (level < LogLevel.Error) throwable else throwable ?: Exception() //to print stacktrace
        return "${level.toString().padEnd(5)} | ${category.substringAfterLast('.').padEnd(25)} | ${message?.toString()
            ?: ""} ${throwableToPrint?.getThrowableText()?.let { "| $it" } ?: ""}"
    }

    /**
     * Drop messages related to RdExtBase and replace all RdId occurrences to empty string
     */
    fun processMessage(message: Any?): String? {
        val prohibitedPatterns = listOf("\\(\\d+\\)", "taskId=\\d+", "send request '\\d+'", "task '\\d+'").joinToString(separator = "|") { "($it)" }
        return message?.toString()?.takeIf { !it.contains(excludedRegex) }?.replace(Regex(prohibitedPatterns), "")
    }

    override fun getLogger(category: String) = object : Logger {
        override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
            if (message != null) {
                if (category in includedCategories) {
                    processMessage(message)?.let {
                        val msg = logFormat(category, level, it, throwable)
                        buffer.add(msg)
                    }
                }
            }
        }

        override fun isEnabled(level: LogLevel) = true
    }

    override fun toString(): String {
        return buffer.joinToString(separator = eol)
    }
}

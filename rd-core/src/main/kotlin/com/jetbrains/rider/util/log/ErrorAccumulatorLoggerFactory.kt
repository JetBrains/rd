package com.jetbrains.rider.util.log

import com.jetbrains.rider.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object ErrorAccumulatorLoggerFactory : ILoggerFactory {
    var warnAsErrors = false

    val errors = ConcurrentLinkedQueue<String>()

    override fun getLogger(category: String): Logger = object : Logger {
        override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
            if (isEnabled(level)) {
                val renderMessage = "$level | $message | "+ throwable?.getThrowableText()
                errors.add(renderMessage)
            }
        }

        override fun isEnabled(level: LogLevel): Boolean {
            return level == LogLevel.Fatal || level == LogLevel.Error || (warnAsErrors && level == LogLevel.Warn)
        }
    }

    fun throwAndClear() {
        //todo
    }
}
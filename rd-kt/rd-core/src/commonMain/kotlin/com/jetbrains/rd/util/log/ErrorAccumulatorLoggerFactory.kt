package com.jetbrains.rd.util.log

import com.jetbrains.rd.util.*

object ErrorAccumulatorLoggerFactory : ILoggerFactory {

    var warnAsErrors = false

    val errors = mutableListOf<String>()
    val creationThread = currentThreadName()


    override fun getLogger(category: String): Logger = object : Logger {
        private val consoleLogger by lazy { ConsoleLoggerFactory.getLogger(category) }

        override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {

            if (!shouldBeLoggedAsError(level)) {
                consoleLogger.log(level, message, throwable)
                return
            }

            if (currentThreadName() == creationThread) {
                throw Exception(message?.toString(), throwable)
            }

            val renderMessage = defaultLogFormat(category, level, message, throwable)
            Sync.lock(this) {
                errors.add(renderMessage)
            }
        }

        override fun isEnabled(level: LogLevel): Boolean = consoleLogger.isEnabled(level)

        fun shouldBeLoggedAsError(level: LogLevel): Boolean {
            return level == LogLevel.Fatal || level == LogLevel.Error || (warnAsErrors && level == LogLevel.Warn)
        }
    }

    fun throwAndClear() {
        if (errors.isEmpty()) return

        val text = "There are ${errors.size} exceptions:$eol" +
                errors.joinToString("$eol$eol --------------------------- $eol$eol")
        errors.clear()
        throw Exception(text)
    }
}
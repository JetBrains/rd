package com.jetbrains.rider.util

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.viewableTail
import kotlin.reflect.KClass

enum class LogLevel {
    Trace,
    Debug,
    Info,
    Warn,
    Error,
    Fatal
}

interface Logger {
    fun log(level: LogLevel, message: Any?, throwable: Throwable?)
    fun isEnabled(level: LogLevel): Boolean
}

interface ILoggerFactory {
    fun getLogger(category: String): Logger
}


class SwitchLogger(category: String) : Logger {
    private lateinit var realLogger: Logger

    init {
        Statics<ILoggerFactory>().stack.viewableTail().advise(Lifetime.Eternal) { factory ->
            realLogger = (factory?:ConsoleLoggerFactory).getLogger(category)
        }
    }

    override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
        realLogger.log(level, message, throwable)
    }

    override fun isEnabled(level: LogLevel): Boolean = realLogger.isEnabled(level)

}


object ConsoleLoggerFactory : ILoggerFactory {
    var level : LogLevel = LogLevel.Trace
    override fun getLogger(category: String) = object : Logger {
        override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
            if (!isEnabled(level)) return
            println("$level | $category | ${message?.toString()} ${throwable?.getThrowableText()}")
        }

        override fun isEnabled(level: LogLevel): Boolean = level >= this@ConsoleLoggerFactory.level

    }
}


fun getLogger(category: String) = SwitchLogger(category)
fun getLogger(categoryKclass: KClass<*>): Logger = getLogger(qualifiedName(categoryKclass))
inline fun <reified T> getLogger() = getLogger(T::class)


inline fun Logger.log(level: LogLevel, msg: () -> Any?) {
    if (isEnabled(level)) {
        log(level, msg(), null)
    }
}

inline fun Logger.trace(msg: () -> Any?) = log(LogLevel.Trace, msg)
inline fun Logger.debug(msg: () -> Any?) = log(LogLevel.Debug, msg)
inline fun Logger.info(msg: () -> Any?)  = log(LogLevel.Info, msg)
inline fun Logger.warn(msg: () -> Any?)  = log(LogLevel.Warn, msg)
inline fun Logger.error(msg: () -> Any?) = log(LogLevel.Error, msg)

fun Logger.error(msg: String?, throwable: Throwable) = log(LogLevel.Error, msg, throwable)
fun Logger.error(throwable: Throwable) = this.error(null, throwable)

inline fun Logger.catch(comment: String?, action:() -> Unit) {
    try {
        action()
    } catch (e : Throwable) {
        val sfx = if (comment.isNullOrBlank()) "" else ": $comment"
        error("Catch$sfx", e)
    }
}
inline fun Logger.catch(action:() -> Unit) = catch (null, action)

inline fun catch(comment: String?, action:() -> Unit) {
    try {
        action()
    } catch (e : Throwable) {
        val sfx = if (comment.isNullOrBlank()) "" else ": $comment"
        getLogger("Default-Error-Logger").log(LogLevel.Error, "Catch$sfx", e)
    }
}
inline fun catch(action:() -> Unit) = catch (null, action)

package com.jetbrains.rider.util

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

//jvm impl backed by commons-logging
internal object DefaultLoggerFactory : ILoggerFactory {
    override fun getLogger(category: String): Logger = object : Logger {
        private val internalLogger = org.apache.commons.logging.LogFactory.getLog(category)

        override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
            when (level) {
                LogLevel.Trace  -> internalLogger.trace(message, throwable)
                LogLevel.Debug  -> internalLogger.debug(message, throwable)
                LogLevel.Info   -> internalLogger.info(message, throwable)
                LogLevel.Warn   -> internalLogger.warn(message, throwable)
                LogLevel.Error  -> internalLogger.error(message, throwable)
                LogLevel.Fatal  -> internalLogger.fatal(message, throwable)
            }
        }

        override fun isEnabled(level: LogLevel) : Boolean =  when (level) {
            LogLevel.Trace  -> internalLogger.isTraceEnabled
            LogLevel.Debug  -> internalLogger.isDebugEnabled
            LogLevel.Info   -> internalLogger.isInfoEnabled
            LogLevel.Warn   -> internalLogger.isWarnEnabled
            LogLevel.Error  -> internalLogger.isErrorEnabled
            LogLevel.Fatal  -> internalLogger.isFatalEnabled
        }
    }
}



fun getLogger(category: String) = (Static.peek(ILoggerFactory::class) ?: DefaultLoggerFactory).getLogger(category)
fun getLogger(categoryKlass: KClass<*>): Logger = getLogger(categoryKlass.qualifiedName ?: "Undefined-Logger")
inline fun <reified T> getLogger() = getLogger(T::class)
fun Any.getCurrentClassLogger() = getLogger(this::class)


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

package com.jetbrains.rd.util

import com.jetbrains.rd.util.lifetime.Lifetime
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
    companion object {
        val root = getLogger("")

        fun set(lifetime: Lifetime, logger: ILoggerFactory) {
            lifetime.onTermination ( //careful - this executes instantly and returns disposable which is added by onTermination
                Statics<ILoggerFactory>().push(logger)
            )
        }
    }
    fun log(level: LogLevel, message: Any?, throwable: Throwable?)
    fun isEnabled(level: LogLevel): Boolean
}

interface ILoggerFactory {
    fun getLogger(category: String): Logger
}

class SwitchLogger(private val category: String) : Logger {
    @Volatile
    private var cachedPair: Pair? = null

    override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
        getRealLogger().log(level, message, throwable)
    }

    override fun isEnabled(level: LogLevel): Boolean = getRealLogger().isEnabled(level)

    internal fun getRealLogger(): Logger {
        var pair = cachedPair
        while (true) {
            val currentFactory = LoggerFactoryStatic.currentFactory // currentFactory rarely changes
            if (pair?.factory === currentFactory)
                return pair.logger

            pair = Pair(currentFactory, currentFactory.getLogger(category))
            cachedPair = pair
        }
    }

    private class Pair(val factory: ILoggerFactory, val logger: Logger)
}

private object LoggerFactoryStatic {
    private val factoryHolder = Statics<ILoggerFactory>()
    val currentFactory get() = factoryHolder.get() ?: ConsoleLoggerFactory
}

object ConsoleLoggerFactory : ILoggerFactory {
    var minLevelToLog : LogLevel = LogLevel.Debug
    var levelToLogStderr : LogLevel? = LogLevel.Warn

    var traceCategories = mutableSetOf<String>()

    override fun getLogger(category: String) = object : Logger {

        override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
            if (!isEnabled(level)) return

            val msg = defaultLogFormat(category, level, message, throwable)
            if (levelToLogStderr?.let { level > it } == true)
                printlnError(msg)
            else
                println(msg)
        }

        override fun isEnabled(level: LogLevel): Boolean = level >=  this@ConsoleLoggerFactory.minLevelToLog ||
            traceCategories.contains(category) || traceCategories.contains(category.substringBefore('.'))
    }
}

//This could be changed via Statics or similar plugin technique
fun defaultLogFormat(category: String, level: LogLevel, message: Any?, throwable: Throwable?) : String {
    val throwableToPrint = if (level < LogLevel.Error) throwable  else throwable ?: Exception() //to print stacktrace
    return "${level.toString().padEnd(5)} | ${category.substringAfterLast('.').padEnd(25)} | ${currentThreadName().padEnd(15)} | ${message?.toString()?:""} ${throwableToPrint?.getThrowableText()?.let { "| $it" }?:""}"
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

object RdDefaultErrorLoggerHolder {
    val defaultErrorLogger = getLogger("Default-Error-Logger")
}

inline fun Logger.catch(comment: String?, action:() -> Unit) {
    try {
        action()
    }
    catch (ce: CancellationException) {
        throw ce
    }
    catch (e : Throwable) {
        val sfx = "${e.javaClass.name} ${e.message}" + if (comment.isNullOrBlank()) "" else " ($comment)"
        error("Catch $sfx", e)
    }
}
inline fun Logger.catch(action:() -> Unit) = catch (null, action)

inline fun catchAndDrop(action:() -> Unit) {
    try {
        action()
    } catch (e : Throwable) {
        //nothing
    }
}

inline fun catch(comment: String?, action:() -> Unit) {
    RdDefaultErrorLoggerHolder.defaultErrorLogger.catch(comment, action)
}
inline fun catch(action:() -> Unit) = catch (null, action)

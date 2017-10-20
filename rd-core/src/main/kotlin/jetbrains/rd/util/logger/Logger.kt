package jetbrains.rd.util.logger

header object LoggerFactory {
    fun logger(name: String): Logger
}

enum class LogLevel {
    Trace,
    Debug,
    Info,
    Warn,
    Error
}

inline fun <reified T> logger() = Logger.logger(T::class.qualifiedName ?: "<name>")
fun logger(name: String) = Logger.logger(name)

abstract class Logger {

    companion object {
        fun logger(name: String) = LoggerFactory.logger(name)
    }

    abstract fun log(level: LogLevel, message: String, throwable: Throwable? = null)
}

fun Logger.info(message: String) = log(LogLevel.Info, message)
fun Logger.trace(message: String) = log(LogLevel.Trace, message)
fun Logger.debug(message: String) = log(LogLevel.Debug, message)

fun Logger.error(message: String, throwable: Throwable? = null) = log(LogLevel.Error, message, throwable)
fun Logger.warn(message: String, throwable: Throwable? = null) = log(LogLevel.Warn, message, throwable)

inline fun assert(cond: Boolean, lazyMessage: () -> Any) {
    if (!cond)
        error(lazyMessage())
}

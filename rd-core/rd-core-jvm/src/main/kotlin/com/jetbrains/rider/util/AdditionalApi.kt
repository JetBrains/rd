package com.jetbrains.rider.util

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.reactive.ISource
import java.time.Duration
import java.util.*
import kotlin.concurrent.schedule

private val timer by lazy { Timer("rd throttler", true) }

fun <T : Any> ISource<T>.throttleLast(timeout: Duration, scheduler: IScheduler) = object : ISource<T> {

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        var currentTask: TimerTask? = null
        val lastValue = AtomicReference<T?>(null)

        if (lifetime.isTerminated) return
        lifetime += { currentTask?.cancel() }

        this@throttleLast.advise(lifetime) { v ->
            if (lastValue.getAndSet(v) == null) {
                currentTask = timer.schedule(timeout.toMillis()) {
                    val toSchedule = lastValue.getAndSet(null)?: return@schedule
                    scheduler.invokeOrQueue {
                        if (!lifetime.isTerminated)
                            handler(toSchedule)
                    }
                }
            }
        }

    }
}


//jvm impl backed by commons-logging
internal object CommonsLoggingLoggerFactory : ILoggerFactory {
    override fun getLogger(category: String): Logger = object : Logger {
        private val internalLogger = org.apache.commons.logging.LogFactory.getLog(category)

        override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
            when (level) {
                LogLevel.Trace -> internalLogger.trace(message, throwable)
                LogLevel.Debug -> internalLogger.debug(message, throwable)
                LogLevel.Info -> internalLogger.info(message, throwable)
                LogLevel.Warn -> internalLogger.warn(message, throwable)
                LogLevel.Error -> internalLogger.error(message, throwable)
                LogLevel.Fatal -> internalLogger.fatal(message, throwable)
            }
        }

        override fun isEnabled(level: LogLevel) : Boolean =  when (level) {
            LogLevel.Trace -> internalLogger.isTraceEnabled
            LogLevel.Debug -> internalLogger.isDebugEnabled
            LogLevel.Info -> internalLogger.isInfoEnabled
            LogLevel.Warn -> internalLogger.isWarnEnabled
            LogLevel.Error -> internalLogger.isErrorEnabled
            LogLevel.Fatal -> internalLogger.isFatalEnabled
        }
    }
}

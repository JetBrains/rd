package com.jetbrains.rd.util

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.plusAssign
import com.jetbrains.rd.util.reactive.IPropertyView
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ISource
import java.time.Duration
import java.util.*
import java.util.EnumSet
import kotlin.concurrent.schedule

private val timer by lazy { Timer("rd throttler", true) }

fun <T : Any> ISource<T>.throttleLast(timeout: Duration, scheduler: IScheduler) = object : ISource<T> {

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        var currentTask: TimerTask? = null
        val lastValue = AtomicReference<T?>(null)

        lifetime.executeIfAlive {
            lifetime += { currentTask?.cancel() }
        }


        this@throttleLast.advise(lifetime) { v ->
            if (lastValue.getAndSet(v) == null) {
                currentTask = timer.schedule(timeout.toMillis()) {
                    val toSchedule = lastValue.getAndSet(null)?: return@schedule
                    scheduler.invokeOrQueue {
                        lifetime.executeIfAlive {
                            handler(toSchedule)
                        }
                    }
                }
            }
        }

    }
}


fun <T : Any> IPropertyView<T?>.debounceNotNull(timeout: Duration, scheduler: IScheduler) = object : ISource<T?> {

    override fun advise(lifetime: Lifetime, handler: (T?) -> Unit) {
        var currentTask: TimerTask? = null

        this@debounceNotNull.view(lifetime) { innerLifetime, v ->
            currentTask?.cancel()

            if (v == null)
                handler(v)
            else
                currentTask = timer.schedule(timeout.toMillis()) {
                    scheduler.invokeOrQueue {
                        innerLifetime.executeIfAlive {
                            handler(v)
                        }
                    }
                }
        }
    }
}

fun <T> ISource<T>.asProperty(defaultValue: T) = object : IPropertyView<T> {
    override val change: ISource<T>
        get() = this@asProperty
    override var value: T = defaultValue
        private set

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) = super.advise(lifetime) {
        value = it
        handler(it)
    }
}



//jvm impl backed by commons-logging
object CommonsLoggingLoggerFactory : ILoggerFactory {
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

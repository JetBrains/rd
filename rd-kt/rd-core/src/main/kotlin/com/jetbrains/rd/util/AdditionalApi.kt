package com.jetbrains.rd.util

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.threading.SynchronousScheduler
import com.jetbrains.rd.util.threading.coroutines.asCoroutineDispatcher
import com.jetbrains.rd.util.threading.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.time.Duration
import java.util.*
import kotlin.concurrent.schedule

private val timer by lazy { Timer("rd throttler", true) }

fun <T : Any> ISource<T>.throttleLast(timeout: Duration, scheduler: IScheduler) = object : ISource<T> {

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        adviseOn(lifetime, scheduler, handler)
    }

    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (T) -> Unit) {
        val flow = MutableSharedFlow<T>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        this@throttleLast.adviseOn(lifetime, SynchronousScheduler) {
            flow.tryEmit(it)
        }

        lifetime.launch(scheduler.asCoroutineDispatcher) {
            flow.sample(timeout.toMillis()).collect {
                Logger.root.catch { handler(it) }
            }
        }
    }
}


fun <T : Any> IPropertyView<T?>.debounceNotNull(timeout: Duration, scheduler: IScheduler) = object : ISource<T?> {

    override fun advise(lifetime: Lifetime, handler: (T?) -> Unit) {
        adviseOn(lifetime, scheduler, handler)
    }

    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (T?) -> Unit) {
        val flow = MutableSharedFlow<T>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        this@debounceNotNull.adviseOn(lifetime, scheduler) {
            if (it == null)
                handler(null)
            else
                flow.tryEmit(it)
        }

        lifetime.launch(scheduler.asCoroutineDispatcher) {
            flow.debounce(timeout.toMillis()).collect {
                Logger.root.catch { handler(it) }
            }
        }
    }
}


fun <T> ISource<T>.asProperty(lifetime: Lifetime, defaultValue: T): IPropertyView<T> {
    return Property(defaultValue).apply {
        this@asProperty.flowInto<T>(lifetime, this@apply)
    }
}

/**
 * Converts an [ISource] into an [IPropertyView] with a default value.
 *
 * This method expects that the original source is stateless and doesn't provide a full-fledged property as a result.
 * The `value` of the property is changed only if there is at least one subscription.
 *
 * @param defaultValue The default value to be used if no other value is emitted.
 * @return An [IPropertyView] representing the converted source.
 */
@DelicateRdApi
fun <T> ISource<T>.asProperty(defaultValue: T) = object : IPropertyView<T> {
    override val change: ISource<T>
        get() = this@asProperty
    override var value: T = defaultValue
        private set

    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) = super.advise(lifetime) {
        value = it
        handler(it)
    }

    override fun adviseOn(lifetime: Lifetime, scheduler: IScheduler, handler: (T) -> Unit) {
        throw UnsupportedOperationException()
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

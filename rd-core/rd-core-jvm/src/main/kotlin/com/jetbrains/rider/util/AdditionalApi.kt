package com.jetbrains.rider.util

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.reactive.ISource
import java.io.PrintWriter
import java.io.StringWriter
import java.net.InetAddress
import java.net.ServerSocket
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

data class PortPair private constructor(val senderPort: Int, val receiverPort: Int) {
    companion object {
        fun getFree(senderPort: Int = 55500, receiverPort: Int = 55501): PortPair {
            val (actualSenderPort, actualReceiverPort) = NetUtils.findFreePorts(senderPort, receiverPort)
            return PortPair(actualSenderPort, actualReceiverPort)
        }
    }
}

object NetUtils {
    private fun isPortFree(port: Int?): Boolean {
        if (port == null) return true
        var socket: ServerSocket? = null
        try {
            socket = ServerSocket(port, 0, InetAddress.getByName("127.0.0.1"))
            socket.reuseAddress = true
            return true
        } catch (e: Exception) {
            return false
        } finally {
            socket?.close()
        }
    }

    fun findFreePort(port: Int): Int {
        if (port > 0 && isPortFree(port))
            return port
        val socket1 = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))
        val result = socket1.localPort
        socket1.reuseAddress = true;
        socket1.close();
        return result
    }

    internal fun findFreePorts(senderPort: Int, receiverPort: Int): Pair<Int, Int> {
        return Pair(findFreePort(senderPort), findFreePort(receiverPort))
    }
}
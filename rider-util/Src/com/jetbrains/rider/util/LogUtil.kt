package com.jetbrains.rider.util

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

inline fun catch(action:() -> Unit) {
    try {
        action()
    } catch (e : Throwable) {
        LogFactory.getLog("catch").error("Isolated exception", e)
    }
}

inline fun catch(comment: String?, action:() -> Unit) {
    try {
        action()
    } catch (e : Throwable) {
        val sfx = if (comment.isNullOrBlank()) "" else ": $comment"
        LogFactory.getLog("catch").error("Isolated exception$sfx", e)
    }
}

inline fun catchWarning(warning: String, action:() -> Unit) {
    try {
        action()
    } catch (e : Throwable) {
        LogFactory.getLog("catch").warn(warning, e)
    }
}

inline fun Maybe.Just<Log>.error(message: () -> Any?) {
    if (value.isErrorEnabled) value.error(message())
}

inline fun Maybe.Just<Log>.warn(message: () -> Any?) {
    if (value.isWarnEnabled) value.warn(message())
}

inline fun Maybe.Just<Log>.info(message: () -> Any?) {
    if (value.isInfoEnabled) value.info(message())
}

inline fun Maybe.Just<Log>.debug(message: () -> Any?) {
    if (value.isDebugEnabled) value.debug(message())
}

inline fun Maybe.Just<Log>.trace(message: () -> String) {
    if (value.isTraceEnabled) value.trace(message())
}
package com.jetbrains.rd.framework.util

import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.threading.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

@Deprecated("Api moved to rd-core", ReplaceWith("asCoroutineDispatcher", "com.jetbrains.rd.util.threading.coroutines.asCoroutineDispatcher"))
val IScheduler.asCoroutineDispatcher get() = asCoroutineDispatcher
@Deprecated("Api moved to rd-core", ReplaceWith("asCoroutineDispatcher(allowInlining)", "com.jetbrains.rd.util.threading.coroutines.asCoroutineDispatcher"))
fun IScheduler.asCoroutineDispatcher(allowInlining: Boolean): CoroutineDispatcher = asCoroutineDispatcher(allowInlining)

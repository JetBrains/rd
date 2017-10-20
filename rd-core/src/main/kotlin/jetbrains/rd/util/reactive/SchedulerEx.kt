package com.jetbrains.rider.util.reactive

import java.util.concurrent.CountDownLatch


fun IScheduler.flushScheduler() {
    val finishSignal = CountDownLatch(1)
    queue({ finishSignal.countDown() })

    finishSignal.await()
}

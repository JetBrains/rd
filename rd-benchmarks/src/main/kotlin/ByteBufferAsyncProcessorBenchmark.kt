package com.jetbrains.rd.benchmarks

import com.jetbrains.rider.util.threading.ByteBufferAsyncProcessor
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

fun benchmarkByteBufferAsyncProcessorShortLoop(timeToRunMs: Int, numSendThreads: Int, sendSize: Int, chunkSize: Int, maxChunks: Int): PerfReport {
    val benchmarkName = "BBAP-shortLoop nt=$numSendThreads, ss=$sendSize, cs=$chunkSize, mc=$maxChunks"
    println("run $benchmarkName")

    var receivedBytes = 0L
    val buffer = ByteBufferAsyncProcessor("BenchmarkProcessor", chunkSize, maxChunks) {
        receivedBytes += it.len
    }

    val sendBuf = ByteArray(sendSize)
    var runSend = true
    val sendLatch = CountDownLatch(1)

    val perThreadCounts = LongArray(numSendThreads)

    val sendThreads = (0 until numSendThreads).map {
        thread(name = "Send Thread $it") {
            sendLatch.await()
            while (runSend) {
                buffer.put(sendBuf)
                perThreadCounts[it]++
            }
        }
    }
    buffer.start()
    val startTime = System.nanoTime()
    sendLatch.countDown()
    while(true) {
        if(System.nanoTime() - startTime > timeToRunMs * 1_000_000L)
            break
        Thread.sleep(1)
    }

    runSend = false
    sendThreads.forEach { it.join() }
    buffer.terminate()
    val endTime = System.nanoTime()


    return PerfReport(benchmarkName, receivedBytes, endTime - startTime, perThreadCounts.sum().toInt())
}

fun runBBAPBenchmarks(isWarmup: Boolean): String {
    val baseTime = if(isWarmup) 100 else 5_000
    val builder = StringBuilder()

    for(sendTreads in arrayOf(1, 3, 7))
        for(maxChunks in arrayOf(2, 10))
            for(chunkSize in arrayOf(65536/4, 65536))
                for(sendSize in arrayOf(1, 100, 10_000))
                    builder.appendln(benchmarkByteBufferAsyncProcessorShortLoop(baseTime, sendTreads, sendSize, chunkSize, maxChunks).statsString())

    return builder.toString()
}

fun main() {
    val resultBefore = runBBAPBenchmarks(true)

    val resultAfter = runBBAPBenchmarks(false)

    println("Result before:")
    println(resultBefore)
    println("\n\n\nResult after:")
    println(resultAfter)
}
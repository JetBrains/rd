//package com.jetbrains.rider.util.test.cases
//
//import com.jetbrains.rider.util.lifetime.Lifetime
//import com.jetbrains.rider.util.reactive.*
//import java.util.concurrent.CancellationException
//import kotlin.concurrent.thread
//import kotlin.test.assertEquals
//import kotlin.test.assertTrue
//
//class TaskTest {
//
//    private fun <T> runOnBackground(block: () -> T) : Task<T> {
//        val res = Trigger<TaskResult<T>>()
//        thread {
//            res.set(TaskResult.from(block))
//        }
//        return res
//    }
//
//    @Test
//    fun testSyncTask() {
//        val factory = TaskFactory<Int, Int> (SynchronousTaskHandler { i -> i * i })
//        val task = factory.start(2)
//        assertEquals(4, task.result())
//    }
//
//    @Test
//    fun testAsyncTask() {
//        val factory = TaskFactory<Int, Int>().apply { set { _, i -> runOnBackground {
//            Thread.sleep(100)
//            i*i
//        }}}
//        val task = factory.start(2)
//        assertEquals(4, task.result())
//    }
//
//    @Test
//    fun testCancel() {
//        val factory = TaskFactory<Int, Int>().apply { set { lf, i -> runOnBackground {
//            while (true) {
//                Thread.sleep(100)
//                if (lf.isTerminated) throw CancellationException()
//            }
//            i*i
//        }}}
//
//
//        val task = Lifetime.using { factory.start(it, 1) }
//        task.wait()
//        assertTrue { task.isCanceled }
//    }
//}

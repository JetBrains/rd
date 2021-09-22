package com.jetbrains.rd.framework.test.cases.extensions

import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import demo.ClassWithExt
import demo.DemoModel
import demo.classExtModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

class BackgroundExtCreationTest : RdFrameworkTestBase() {
    @Test
    fun testExtCreationFromBackground() {
        val count = 10000
        val timeoutInSeconds = 20L
        
        val serverModel = DemoModel.create(serverLifetime, serverProtocol)
        /* clientModel */ DemoModel.create(clientLifetime, clientProtocol)
        
        val models = (0 until count).map { ClassWithExt(it) }
        models.forEach { serverModel.extList.add(it) }
        val executor = Executors.newFixedThreadPool(5)
        val futureList = models.map { model ->
            executor.submit { model.classExtModel.values.fire(10) }
        }
        executor.shutdown()
        val success = executor.awaitTermination(timeoutInSeconds, TimeUnit.SECONDS)
        assertTrue(success, "Failed to terminate in $timeoutInSeconds seconds")

        futureList.forEach {
            assertDoesNotThrow {
                it.get()
            }
        }
    }
}
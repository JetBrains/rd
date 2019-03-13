package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.log.ErrorAccumulatorLoggerFactory
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.threading.TestSingleThreadScheduler
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import kotlin.test.AfterTest

open class RdAsyncTestBase : RdFrameworkTestBase() {
    companion object {
        var clientBgScheduler: TestSingleThreadScheduler = TestSingleThreadScheduler("ClientBg")
        private set
        var clientUiScheduler: TestSingleThreadScheduler = TestSingleThreadScheduler("ClientUi")
        private set
        var serverBgScheduler: TestSingleThreadScheduler = TestSingleThreadScheduler("ServerBg")
        private set
        var serverUiScheduler: TestSingleThreadScheduler = TestSingleThreadScheduler("ServerUi")
        private set

        @BeforeClass
        @JvmStatic
        fun makeFreshSchedulers() {
            if(!clientBgScheduler.executor.isShutdown) return

            clientBgScheduler = TestSingleThreadScheduler("ClientBg")
            clientUiScheduler = TestSingleThreadScheduler("ClientUi")
            serverBgScheduler = TestSingleThreadScheduler("ServerBg")
            serverUiScheduler = TestSingleThreadScheduler("ServerUi")
        }

        @AfterClass
        @JvmStatic
        fun shutdownSchedulers() {
            clientBgScheduler.executor.shutdown()
            clientUiScheduler.executor.shutdown()
            serverBgScheduler.executor.shutdown()
            serverUiScheduler.executor.shutdown()
        }
    }


    override val clientScheduler: IScheduler
        get() = clientUiScheduler

    override val serverScheduler: IScheduler
        get() = serverUiScheduler

    @Before
    fun clearErrorsBeforeTest() {
        ErrorAccumulatorLoggerFactory.errors.clear()
    }

    @After
    fun reportErrorsAfterTest() {
        ErrorAccumulatorLoggerFactory.throwAndClear()
    }

    @AfterTest
    fun tearDownSchedulers() {
        clientBgScheduler.assertNoExceptions()
        serverBgScheduler.assertNoExceptions()
        clientUiScheduler.assertNoExceptions()
        serverUiScheduler.assertNoExceptions()
    }
}

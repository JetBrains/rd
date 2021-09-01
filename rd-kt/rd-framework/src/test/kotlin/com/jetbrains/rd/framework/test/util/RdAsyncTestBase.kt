package com.jetbrains.rd.framework.test.util

import com.jetbrains.rd.util.log.ErrorAccumulatorLoggerFactory
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.threading.TestSingleThreadScheduler
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach


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

        @BeforeAll
        @JvmStatic
        fun makeFreshSchedulers() {
            if(!clientBgScheduler.executor.isShutdown) return

            clientBgScheduler = TestSingleThreadScheduler("ClientBg")
            clientUiScheduler = TestSingleThreadScheduler("ClientUi")
            serverBgScheduler = TestSingleThreadScheduler("ServerBg")
            serverUiScheduler = TestSingleThreadScheduler("ServerUi")
        }

        @AfterAll
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

    @BeforeEach
    fun clearErrorsBeforeTest() {
        ErrorAccumulatorLoggerFactory.errors.clear()
    }

    @AfterEach
    fun reportErrorsAfterTest() {
        ErrorAccumulatorLoggerFactory.throwAndClear()
    }

    @AfterEach
    fun tearDownSchedulers() {
        clientBgScheduler.assertNoExceptions()
        serverBgScheduler.assertNoExceptions()
        clientUiScheduler.assertNoExceptions()
        serverUiScheduler.assertNoExceptions()
    }
}

package com.jetbrains.rider.framework.test.cases

import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.threading.TestSingleThreadScheduler
import kotlin.test.AfterTest

open class RdAsyncTestBase : RdFrameworkTestBase() {
    val clientBgScheduler: TestSingleThreadScheduler = TestSingleThreadScheduler("ClientBg")
    val clientUiScheduler: TestSingleThreadScheduler = TestSingleThreadScheduler("ClientUi")
    val serverBgScheduler: TestSingleThreadScheduler = TestSingleThreadScheduler("ServerBg")
    val serverUiScheduler: TestSingleThreadScheduler = TestSingleThreadScheduler("ServerUi")

    override val clientScheduler: IScheduler
        get() = clientUiScheduler

    override val serverScheduler: IScheduler
        get() = serverUiScheduler

    @AfterTest
    fun tearDownSchedulers() {
        clientBgScheduler.assertNoExceptions()
        serverBgScheduler.assertNoExceptions()
        clientUiScheduler.assertNoExceptions()
        serverUiScheduler.assertNoExceptions()
    }
}

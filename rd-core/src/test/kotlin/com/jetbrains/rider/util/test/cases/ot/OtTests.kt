package com.jetbrains.rider.util.test.cases.ot

import com.jetbrains.rider.util.ot.*
import com.jetbrains.rider.util.reactive.adviseEternal
import com.jetbrains.rider.util.test.cases.ot.testDataGenerators.*
import com.pholser.junit.quickcheck.Property
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck
import org.junit.Assert
import org.junit.runner.RunWith
import java.util.*


@RunWith(JUnitQuickcheck::class)
@Suppress("UNUSED_VARIABLE")
class OtTests {

    @Property(trials = 1000)
    fun compose(composeTestData: ComposeTestData) {
        //println(composeTestData)
        val (testState, operation1, operation2) = composeTestData
        val composition = com.jetbrains.rider.util.ot.compose(operation1, operation2)
        val x = testState
                .let { play(it, operation1) }
                .let { play(it, operation2) }
        val y = testState
                .let { play(it, composition) }
        Assert.assertTrue(x == y)
    }

    @Property(trials = 1000)
    fun transform(transformTestData: TransformTestData) {
        //println(transformTestData)
        val (testState, operation1, operation2) = transformTestData
        val (transformed1, transformed2) = com.jetbrains.rider.util.ot.transform(operation1, operation2)
        val x = testState.let { play(it, operation1) }
                .let { play(it, transformed2) }
        val y = testState.let { play(it, operation2) }
                .let { play(it, transformed1) }
        Assert.assertTrue(x == y)
    }

    @Property(trials = 1000)
    fun complexTest1(/*@When(seed = 5348405124048101829)*/ ops: ThreeOperationTestData) {
        //println(ops)
        // check equation: T(c, a+b).c1 = T(T(c, a).c1, b).c2
        val (initText, c, a, b) = ops

        val (c1, a1) = transform(c, a)
        val (c2, b1) = transform(c1, b)
        val ab = compose(a, b)
        val (ab1, other_c2) = transform(ab, c)
        val text = initText.let { play(it, a) }
                .let { play(it, b) }

        val x = text.let { play(it, c2) }
        val y = text.let { play(it, other_c2) }
        Assert.assertTrue(x == y)
    }

    @Property(trials = 1000)
    fun playOtRound(/*@When(seed =)*/ round: RandomExecutionPathTestData) {
        val ops = round.track

        val client = OtStateInTest(OtRole.Slave, Text.EMPTY)
        val server = OtStateInTest(OtRole.Master, Text.EMPTY)
        val messageQueue = ArrayDeque<() -> Unit>()
        server.sendAck.adviseEternal { messageQueue.add({ client.receiveAck.fire(it) }) }
        client.sendAck.adviseEternal { messageQueue.add({ server.receiveAck.fire(it) }) }
        server.sendOperation.adviseEternal { messageQueue.add({ client.receiveOperation.fire(it) }) }
        client.sendOperation.adviseEternal { messageQueue.add({ server.receiveOperation.fire(it) }) }

        //println("------------------------------------------------------")
        @Suppress("RemoveForLoopIndices")
        for ((i, turn) in ops.withIndex()) {
            val role = turn.role
            if (role == OtRole.Master) {
                server.sendOperation(turn)
            } else {
                client.sendOperation(turn)
            }

            //println("($i, $role) - apply $turn")
            while (messageQueue.isNotEmpty()) {
                val msg = messageQueue.pop()
                msg.invoke()
            }
            //println("${OtRole.Master} state after: $server")
            //println("${OtRole.Slave} state after: $client")
            Assert.assertTrue(server.text == client.text)
        }
        client.assertEmptyDiff()
        server.assertEmptyDiff()
        //println("------------------------------------------------------")
    }
}
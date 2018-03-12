package com.jetbrains.rider.util.test.cases.ot

import com.jetbrains.rider.util.ot.OtOperation
import com.jetbrains.rider.util.ot.OtRole
import com.jetbrains.rider.util.ot.OtState
import com.jetbrains.rider.util.test.cases.ot.testDataGenerators.Text
import com.jetbrains.rider.util.test.cases.ot.testDataGenerators.play
import org.testng.Assert

class OtStateInTest(role: OtRole, var text: Text) : OtState(role) {

    public override fun sendOperation(op: OtOperation) {
        text = play(text, op)
        super.sendOperation(op)
    }

    override fun applyOperation(op: OtOperation) {
        text = play(text, op)
        super.applyOperation(op)
    }

    override fun toString(): String {
        return super.toString() + ";text: $text"
    }

    fun assertEmptyDiff() = Assert.assertTrue(diff.isIdentity())
}
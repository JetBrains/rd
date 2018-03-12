package com.jetbrains.rider.util.test.cases.ot.testDataGenerators

import com.jetbrains.rider.util.ot.OtOperation
import com.jetbrains.rider.util.ot.OtRole
import com.pholser.junit.quickcheck.generator.GenerationStatus
import com.pholser.junit.quickcheck.generator.Generator
import com.pholser.junit.quickcheck.random.SourceOfRandomness

data class TransformTestData(val text: Text, val op1: OtOperation, val op2: OtOperation)

class TransformTestDataGen : Generator<TransformTestData>(TransformTestData::class.java) {
    override fun generate(r: SourceOfRandomness, s: GenerationStatus): TransformTestData {
        val state = gen().type(Text::class.java).generate(r, s)
        val op1 = gen().nextOp(state, r, s, OtRole.Slave)
        val op2 = gen().nextOp(state, r, s, OtRole.Master)
        return TransformTestData(state, op1, op2)
    }
}
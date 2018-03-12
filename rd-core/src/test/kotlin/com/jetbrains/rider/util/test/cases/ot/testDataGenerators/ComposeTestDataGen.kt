package com.jetbrains.rider.util.test.cases.ot.testDataGenerators

import com.jetbrains.rider.util.ot.OtOperation
import com.pholser.junit.quickcheck.generator.GenerationStatus
import com.pholser.junit.quickcheck.generator.Generator
import com.pholser.junit.quickcheck.random.SourceOfRandomness

data class ComposeTestData(val text: Text, val op1: OtOperation, val op2: OtOperation)

class ComposeTestDataGen : Generator<ComposeTestData>(ComposeTestData::class.java) {
    override fun generate(r: SourceOfRandomness, s: GenerationStatus): ComposeTestData {
        val state1 = gen().type(Text::class.java).generate(r, s)
        val op1 = gen().nextOp(state1, r, s)
        val state2 = play(state1, op1)
        val op2 = gen().nextOp(state2, r, s)
        return ComposeTestData(state1, op1, op2)
    }
}
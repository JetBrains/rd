package com.jetbrains.rider.util.test.cases.ot.testDataGenerators

import com.jetbrains.rider.util.ot.OtOperation
import com.jetbrains.rider.util.ot.OtRole
import com.pholser.junit.quickcheck.generator.GenerationStatus
import com.pholser.junit.quickcheck.generator.Generator
import com.pholser.junit.quickcheck.random.SourceOfRandomness

data class ThreeOperationTestData(val text: Text, val remote: OtOperation, val local1: OtOperation, val local2: OtOperation)

class ThreeOperationTestDataGen : Generator<ThreeOperationTestData>(ThreeOperationTestData::class.java) {
    override fun generate(r: SourceOfRandomness, s: GenerationStatus): ThreeOperationTestData {
        val state = gen().type(Text::class.java).generate(r, s)

        val remote = gen().nextOp(state, r, s, OtRole.Master)

        val local1 = gen().nextOp(state, r, s, OtRole.Slave)
        val state1 = play(state, local1)
        val local2 = gen().nextOp(state1, r, s, OtRole.Slave)
        return ThreeOperationTestData(state, remote, local1, local2)
    }
}

package com.jetbrains.rider.util.test.cases.ot.testDataGenerators

import com.jetbrains.rider.util.ot.InsertText
import com.jetbrains.rider.util.ot.OtOperation
import com.jetbrains.rider.util.ot.OtRole
import com.pholser.junit.quickcheck.generator.GenerationStatus
import com.pholser.junit.quickcheck.generator.Generator
import com.pholser.junit.quickcheck.random.SourceOfRandomness


data class RandomExecutionPathTestData(val track: List<OtOperation>)

class RandomExecutionPathTestDataGen : Generator<RandomExecutionPathTestData>(RandomExecutionPathTestData::class.java) {
    companion object {
        private const val DEFAULT_STEPS = 100
    }

    private val myStepsCount = DEFAULT_STEPS

    override fun generate(r: SourceOfRandomness, s: GenerationStatus): RandomExecutionPathTestData {
        val initState = gen().type(Text::class.java).generate(r, s)
        var currState = initState
        val track = mutableListOf(OtOperation(listOf(InsertText(initState.text)), OtRole.Master))
        var steps = myStepsCount

        while (steps != 0) {
            val op = if (r.nextBoolean()) gen().nextOp(currState, r, s, OtRole.Master)
                     else gen().nextOp(currState, r, s, OtRole.Slave)
            currState = play(currState, op)
            track.add(op)

            if (currState.text.isEmpty()
                    && track.size > 2
                    && track[track.size - 1].isIdentity()
                    && track[track.size - 2].isIdentity()) {
                break
            }
            steps--
        }

        return RandomExecutionPathTestData(track)
    }
}
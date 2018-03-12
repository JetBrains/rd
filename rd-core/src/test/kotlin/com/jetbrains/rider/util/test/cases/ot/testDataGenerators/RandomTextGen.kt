package com.jetbrains.rider.util.test.cases.ot.testDataGenerators

import com.pholser.junit.quickcheck.generator.GenerationStatus
import com.pholser.junit.quickcheck.generator.Generator
import com.pholser.junit.quickcheck.random.SourceOfRandomness

// todo generates text from words
data class Text(val text: String,
                val headSelection: Int,
                val tailSelection: Int) {
    companion object {
        val EMPTY = Text("", 0, 0)
    }
}

class RandomTextGen : Generator<Text>(Text::class.java) {
    override fun generate(r: SourceOfRandomness, s: GenerationStatus): Text {
        val maxTextLength = 50
        val text = nextAlphabetString(1..maxTextLength, r)
        val headSelection = r.nextInt(0, maxTextLength - 1)
        val tailSelection = r.nextInt(0, maxTextLength - 1)
        return Text(text, headSelection, tailSelection)
    }
}
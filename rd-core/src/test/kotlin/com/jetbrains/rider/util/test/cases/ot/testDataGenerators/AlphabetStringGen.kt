package com.jetbrains.rider.util.test.cases.ot.testDataGenerators

import com.pholser.junit.quickcheck.generator.GenerationStatus
import com.pholser.junit.quickcheck.generator.Generator
import com.pholser.junit.quickcheck.random.SourceOfRandomness


data class AlphabetString(val string: String)

class AlphabetStringGen : Generator<AlphabetString>(AlphabetString::class.java) {
    override fun generate(r: SourceOfRandomness, s: GenerationStatus): AlphabetString {
        val c = r.nextInt(1, MAX_STRING_LEN)
        val sb = StringBuilder(c)
        for (i in 0 until c) {
            val randomIndex = r.nextInt(ALL_MY_CHARS.length)
            sb.append(ALL_MY_CHARS[randomIndex])
        }
        return AlphabetString(sb.toString())
    }

    fun nextString(range: IntRange, r: SourceOfRandomness): String {
        val c = r.nextInt(range.start, range.last)
        val sb = StringBuilder(c)
        for (i in 0 until c) {
            val randomIndex = r.nextInt(ALL_MY_CHARS.length)
            sb.append(ALL_MY_CHARS[randomIndex])
        }
        return sb.toString()
    }

    companion object {
        private const val LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz"
        private const val UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val ALL_MY_CHARS = (LOWERCASE_CHARS + UPPERCASE_CHARS)
        private const val MAX_STRING_LEN = 100
    }
}

private val gen by lazy { AlphabetStringGen() }
fun nextAlphabetString(range: IntRange, r: SourceOfRandomness): String {
    return gen.nextString(range, r)
}
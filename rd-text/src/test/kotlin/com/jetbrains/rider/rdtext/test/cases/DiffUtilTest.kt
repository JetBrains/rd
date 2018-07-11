package com.jetbrains.rider.rdtext.test.cases

import com.jetbrains.rider.rdtext.DiffChangeKind
import com.jetbrains.rider.rdtext.computeNaiveLCS
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.jetCheck.PropertyChecker
import kotlin.test.Test
import kotlin.test.assertEquals

class DiffUtilTest {

    @Test
    fun test() {
        PropertyChecker.customized()
                .withIterationCount(100)
                .checkScenarios { ImperativeCommand {
                    val str1 = it.generateValue(Generator.asciiIdentifiers(), "str1 = '%s'")
                    val str2 = it.generateValue(Generator.asciiIdentifiers(), "str2 = '%s'")
                    val path = computeNaiveLCS(str1.length, str2.length, { x, y -> str1[x] == str2[y] })

                    val str3Builder = StringBuilder()
                    for (change in path) {
                        when (change.kind) {
                            DiffChangeKind.EQUAL -> str3Builder.append(str1, change.oldRange.start, change.oldRange.end)
                            DiffChangeKind.INSERTED -> str3Builder.append(str2, change.newRange.start, change.newRange.end)
                            DiffChangeKind.DELETED -> Unit
                            DiffChangeKind.REPLACED -> str3Builder.append(str2, change.newRange.start, change.newRange.end)
                        }
                    }
                    val str3 = str3Builder.toString()
                    it.logMessage("str3 = '$str3'")
                    assertEquals(str2, str3)
                } }
    }
}
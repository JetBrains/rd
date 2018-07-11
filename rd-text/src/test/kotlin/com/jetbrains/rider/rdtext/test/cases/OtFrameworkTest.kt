package com.jetbrains.rider.rdtext.test.cases

import com.jetbrains.rider.rdtext.impl.intrinsics.RdChangeOrigin
import com.jetbrains.rider.rdtext.impl.ot.*
import com.jetbrains.rider.util.Boxed
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.jetCheck.IntDistribution
import org.jetbrains.jetCheck.PropertyChecker
import kotlin.test.Test
import kotlin.test.assertEquals

class OtFrameworkTest {

    @Test
    fun simpleTest() {
        //0 -> "Fooood(1)"
        val ops = listOf(OtOperation(listOf(Retain(3), DeleteText("ood(1)", OtChangePriority.Normal), Retain(0)), RdChangeOrigin.Slave, 0, OtOperationKind.Normal),
                OtOperation(listOf(Retain(3), InsertText("(1)", OtChangePriority.Normal)), RdChangeOrigin.Slave, 0, OtOperationKind.Normal),
                OtOperation(listOf(Retain(0), InsertText("_\t", OtChangePriority.Normal), DeleteText("Foo", OtChangePriority.Normal), Retain(3)), RdChangeOrigin.Slave, 0, OtOperationKind.Normal),
                OtOperation(listOf(Retain(0), InsertText("Foo", OtChangePriority.Normal), DeleteText("_", OtChangePriority.Normal), Retain(4)), RdChangeOrigin.Slave, 0, OtOperationKind.Normal),
                OtOperation(listOf(Retain(3), DeleteText("\t", OtChangePriority.Normal), Retain(3)), RdChangeOrigin.Slave, 0, OtOperationKind.Normal))

        val composedOp = ops.subList(1, ops.size).fold(ops.first(), { acc, x -> compose(acc, x) })
        val expectedOp = OtOperation(listOf(Retain(3), DeleteText("ood", OtChangePriority.Normal), Retain(3)), RdChangeOrigin.Slave, 0, OtOperationKind.Normal)
        assertEquals(expectedOp, composedOp)
    }

    private fun otOperation(textRef: Boxed<String>): Generator<OtOperation> {
        return Generator.from {
            var offset = 0
            val changes = mutableListOf<OtChange>()
            var text = textRef.value
            while (offset != text.length ) {
                val otChangeCode = it.generate(Generator.integers(0, 2))
                when (otChangeCode) {
                    0 -> {
                        val max = text.length - offset
                        if (max > 0) {
                            val shift = it.generate(Generator.integers(1, max))
                            offset += shift
                            changes.add(Retain(shift))
                        }
                    }
                    1 -> {
                        val insertText = it.generate(Generator.stringsOf(IntDistribution.uniform(1, 10), Generator.asciiLetters()))
                        text = text.substring(0, offset) + insertText + text.substring(offset)
                        offset += insertText.length
                        changes.add(InsertText(insertText, OtChangePriority.Normal))
                    }
                    2 -> {
                        val max = text.length - offset
                        if (max > 0) {
                            val length = it.generate(Generator.integers(1, max))
                            val deletedText = text.substring(offset, offset + length)
                            text = text.substring(0, offset) + text.substring(offset + length)
                            changes.add(DeleteText(deletedText, OtChangePriority.Normal))
                        }
                    }
                    else -> throw IllegalArgumentException()
                }
            }
            textRef.value = text
            OtOperation(changes, RdChangeOrigin.Slave, 0, OtOperationKind.Normal)
        }
    }

    private fun applyOp(operation: OtOperation, text0: String): String {
        var offset = 0
        var text = text0
        for (change in operation.changes) {
            when (change) {
                is Retain -> offset += change.offset
                is InsertText -> {
                    text = (if (offset > 0) text.substring(0, offset) else "") + change.text +
                            (if (offset < text.length) text.substring(offset) else "")
                    offset += change.text.length
                }
                is DeleteText -> {
                    val idx = offset + change.text.length
                    text = ( text.substring(0, offset)) + (text.substring(idx))
                }
            }
        }
        return text
    }

    @Test
    fun testCompose() {
        PropertyChecker.customized()
                .withIterationCount(100)
                .checkScenarios {
                    ImperativeCommand {
                        val initText = it.generateValue(Generator.stringsOf(Generator.asciiLetters()), null)
                        val text = Boxed(initText)

                        val ops: MutableList<OtOperation> = it.generateValue(Generator.listsOf(IntDistribution.uniform(2, 10), otOperation(text)), null)

                        val composedOp = ops.subList(1, ops.size).fold(ops.first(), { acc, x -> compose(acc, x) })
                        val expectedText = ops.fold(initText, { acc, op -> applyOp(op, acc) })
                        val actualText = applyOp(composedOp, initText)
                        assertEquals(expectedText, actualText)
                    }
                }
    }
}
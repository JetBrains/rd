package com.jetbrains.rider.util.test.cases.ot

import com.jetbrains.rider.util.ot.*
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import kotlin.test.assertEquals


class OtOperationTest {

    @DataProvider(name = "normalizeCases")
    fun normalizeCases() = arrayOf(
            arrayOf(listOf(Retain(0)), emptyList()),
            arrayOf(listOf(InsertText("abc"), Retain(0)), listOf(InsertText("abc"))),
            arrayOf(listOf(Retain(2), Retain(3), Retain(1)), emptyList()),
            arrayOf(listOf(Retain(2), Retain(3), InsertText("abc"), InsertText("def")),
                    listOf(Retain(5), InsertText("abcdef"))),
            arrayOf(listOf(Retain(1), DeleteText("abc"), DeleteText("def")),
                    listOf(Retain(1), DeleteText("abcdef"))),
            arrayOf(listOf(Retain(1), DeleteText("abc"), InsertText("def")),
                    listOf(Retain(1), DeleteText("abc"), InsertText("def"))),
            arrayOf(listOf(Retain(1), DeleteText("abc"), DeleteText("def"), InsertText("ghi")),
                    listOf(Retain(1), DeleteText("abcdef"), InsertText("ghi"))),
            arrayOf(listOf(Retain(1), Retain(2), InsertText("q"), DeleteText("abc"), DeleteText("def"), InsertText("ghi")),
                    listOf(Retain(3), InsertText("q"), DeleteText("abcdef"), InsertText("ghi")))
    )

    @Test(dataProvider = "normalizeCases")
    fun testNormalize(originalChanges: List<OtChange>, normalizedChanges: List<OtChange>) {
        val op = OtOperation(originalChanges, OtRole.Slave)
        assertEquals(op.changes, normalizedChanges)
    }

    @DataProvider(name = "operationToTextChange")
    fun operationToTextChangeCases() = arrayOf(
            arrayOf(OtOperation(listOf(Retain(10), DeleteText("old"), InsertText("new"), Retain(10)), OtRole.Slave),
                    listOf(TextChange(10, "old", "new", 23))),
            arrayOf(OtOperation(listOf(Retain(10), InsertText("new"), Retain(10)), OtRole.Slave),
                    listOf(TextChange(10, "", "new", 23))),
            arrayOf(OtOperation(listOf(Retain(10), DeleteText("old"), Retain(10)), OtRole.Slave),
                    listOf(TextChange(10, "old", "", 20))),
            arrayOf(OtOperation(listOf(Retain(10), DeleteText("old"), InsertText("new"), DeleteText("old1"),
                    InsertText("new1"), Retain(10)), OtRole.Slave),
                    listOf(TextChange(10, "old", "new", 27),
                           TextChange(13, "old1", "new1", 27)))
    )

    @Test(dataProvider = "operationToTextChange")
    fun testOperationToTextChange(op: OtOperation, expectedChanges: List<TextChange>) {
        val changes = op.toTextChanges()
        Assert.assertEquals(changes, expectedChanges)
    }

    @DataProvider(name = "textChangeToOperation")
    fun textChangeToOperationCases() = arrayOf(
            arrayOf(TextChange(1, "old", "new", 4),
                    OtOperation(listOf(Retain(1), InsertText("new"), DeleteText("old")), OtRole.Slave))
    )

    @Test(dataProvider = "textChangeToOperation")
    fun testTextChangeToOperation(textChange: TextChange, expectedOperation: OtOperation) {
        val operation = textChange.toOperation(OtRole.Slave)
        Assert.assertEquals(operation, expectedOperation)
    }
}
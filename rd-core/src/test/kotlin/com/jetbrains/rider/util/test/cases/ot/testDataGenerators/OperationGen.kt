package com.jetbrains.rider.util.test.cases.ot.testDataGenerators

import com.jetbrains.rider.util.ot.*
import com.pholser.junit.quickcheck.generator.*
import com.pholser.junit.quickcheck.random.SourceOfRandomness
import java.util.*

sealed class OpStub
class RetainStub(@InRange(minInt = 1, maxInt = 10) val offset: Int) : OpStub()
class DeleteStub(@InRange(minInt = 1, maxInt = 10) val offset: Int) : OpStub()
class InsertStub(val text: String) : OpStub()


class OpStubGen : Generator<OpStub>(OpStub::class.java) {
    override fun generate(r: SourceOfRandomness, status: GenerationStatus): OpStub {
        val nextInt = r.nextInt(1, 3)
        return when (nextInt) {
            1 -> gen().constructor(RetainStub::class.java, Int::class.java).generate(r, status)
            2 -> InsertStub(nextAlphabetString(1..10, r))
            3 -> gen().constructor(DeleteStub::class.java, Int::class.java).generate(r, status)
            else -> throw IllegalAccessException("Unexpected $nextInt")
        }
    }
}

data class OpFrame(@Size(min = 1, max = 20) val ops: ArrayList<OpStub>)

class OpFrameGen : Generator<OpFrame>(OpFrame::class.java) {
    override fun generate(r: SourceOfRandomness?, status: GenerationStatus?): OpFrame =
            gen().constructor(OpFrame::class.java, ArrayList::class.java).generate(r, status)
}

fun Generators.nextOp(state: Text, r: SourceOfRandomness, s: GenerationStatus, role: OtRole = OtRole.Slave): OtOperation {
    val res = ArrayList<OtChange>()

    if (state.text.isEmpty()) return OtOperation(emptyList(), role)

    while (true) { // id elements isn't interested
        var text = state.text
        val f = this.type(OpFrame::class.java).generate(r, s)
        for (op in f.ops) {
            if (text.isEmpty()) break
            when (op) {
                is RetainStub -> {
                    val arg = Math.min(op.offset, text.length)
                    res.add(Retain(arg))
                    text = text.substring(arg)
                }
                is InsertStub -> res.add(InsertText(op.text))
                is DeleteStub -> {
                    val arg = Math.min(op.offset, text.length)
                    res.add(DeleteText(text.substring(0, arg)))
                    text = text.substring(arg)
                }
            }
        }
        if (text.isNotEmpty()) res += Retain(text.length)

        val operation = OtOperation(res, role)
        if (!operation.isIdentity()) {
            return operation
        }
        res.clear()
    }
}

fun play(text: Text, operation: OtOperation): Text {
    val (idx, state) = operation.changes.fold(0 to text) { pair, op ->
        val (idx, state) = pair
        when (op) {
            is Retain -> (op.offset + idx) to state
            is DeleteText -> idx to Text(state.text.substring(0, idx) + state.text.substring(idx + op.text.length),
                    state.headSelection, state.tailSelection)
            is InsertText -> (idx + op.text.length) to Text(state.text.substring(0, idx) + op.text + state.text.substring(idx),
                    state.headSelection, state.tailSelection)
            else -> throw IllegalArgumentException("op")
        }
    }
    assert(operation.isIdentity() || idx == state.text.length,
            { "idx($idx) == state.text.length(${state.text.length})" })
    return state
}

private fun getTestState(operation: OtOperation): Text =
        operation.changes.fold(Text("", 0, 0)) { state, op ->
            when (op) {
                is InsertText -> state
                is DeleteText -> Text(state.text + op.text, 0, 0)
                is Retain -> Text(state.text + buildString { repeat(op.offset, { this@buildString.append(' ') }) }, 0, 0)
                else -> throw IllegalArgumentException("op")
            }
        }
package com.jetbrains.rd.rdtext.impl.ot

import com.jetbrains.rd.util.collections.ImmutableStack
import com.jetbrains.rd.util.collections.tail
import com.jetbrains.rd.util.collections.toImmutableStack

private fun <T> MutableList<T>.append(el: T): MutableList<T> = this.apply { add(el) }

/**
 * Not commutative composition function.
 *
 * Notes: Use it if you want to merge a lot not optimized changes into one.
 * But all of them must have the same logical timestamp of IDE state (for example they must be generated during the same write action).
 */
fun compose(o1: OtOperation, o2: OtOperation): OtOperation {

    val after = o1.documentLengthAfter()
    val before = o2.documentLengthBefore()
    require(after == before, { "length after o1($after) != length before o2($before)" })
    require(o1.origin == o2.origin)
    require(o1.timestamp == o2.timestamp)
    require(o1.kind == o2.kind)

    tailrec fun compose0(acc: MutableList<OtChange>, ops1: ImmutableStack<OtChange>, ops2: ImmutableStack<OtChange>) {
        val op1 = ops1.peek()
        val op2 = ops2.peek()

        val tail1 = ops1.tail()
        val tail2 = ops2.tail()
        when {
            op1 == null && op2 == null -> Unit
            op1 != null && op2 == null -> compose0(acc.append(op1), tail1, tail2)
            op1 == null && op2 != null -> compose0(acc.append(op2), tail1, tail2)
            op1 is DeleteText && op2 is InsertText && op1.text == op2.text -> compose0(acc.append(Retain(op1.text.length)), tail1, tail2)
            op2 is InsertText -> compose0(acc.append(op2), ops1, tail2)
            op1 is DeleteText -> compose0(acc.append(op1), tail1, ops2)
            op1 is Retain && op2 is Retain -> {
                val offset1 = op1.offset
                val offset2 = op2.offset
                when {
                    offset1 > offset2 -> compose0(acc.append(op2), tail1.push(Retain(offset1 - offset2)), tail2)
                    offset1 == offset2 -> compose0(acc.append(op1), tail1, tail2)
                    offset1 < offset2 -> compose0(acc.append(op1), tail1, tail2.push(Retain(offset2 - offset1)))
                    else -> throw IllegalArgumentException("op1 is Retain && op2 is Retain")
                }
            }
            op1 is InsertText && op2 is DeleteText -> {
                val text1 = op1.text
                val text2 = op2.text
                when {
                    text1.length > text2.length -> {
                        require(text1.startsWith(text2))
                        compose0(acc, tail1.push(InsertText(text1.substring(text2.length))), tail2)
                    }
                    text1.length == text2.length -> {
                        require(text1 == text2)
                        compose0(acc, tail1, tail2)
                    }
                    text1.length < text2.length -> {
                        require(text2.startsWith(text1))
                        compose0(acc, tail1, tail2.push(DeleteText(text2.substring(text1.length))))
                    }
                    else -> throw IllegalArgumentException("op1 is InsertText && op2 is DeleteText")
                }
            }
            op1 is InsertText && op2 is Retain -> {
                val text1 = op1.text
                val offset2 = op2.offset
                when {
                    text1.length > offset2 -> compose0(acc.append(InsertText(text1.substring(0, offset2))),
                            tail1.push(InsertText(text1.substring(offset2))), tail2)
                    text1.length == offset2 -> compose0(acc.append(op1), tail1, tail2)
                    text1.length < offset2 -> compose0(acc.append(op1), tail1, tail2.push(Retain(offset2 - text1.length)))
                    else -> throw IllegalArgumentException("op1 is InsertText && op2 is DeleteText")
                }
            }
            op1 is Retain && op2 is DeleteText -> {
                val offset1 = op1.offset
                val text2 = op2.text
                when {
                    offset1 > text2.length -> compose0(acc.append(op2), tail1.push(Retain(offset1 - text2.length)), tail2)
                    offset1 == text2.length -> compose0(acc.append(op2), tail1, tail2)
                    offset1 < text2.length -> compose0(acc.append(DeleteText(text2.substring(0, offset1))),
                            tail1, tail2.push(DeleteText(text2.substring(offset1))))
                    else -> throw IllegalArgumentException("op1 is InsertText && op2 is DeleteText")
                }
            }
            else -> throw IllegalArgumentException("Not matched pair: ($op1, $op2)")
        }
    }

    val acc = mutableListOf<OtChange>()
    compose0(acc, o1.changes.toImmutableStack(), o2.changes.toImmutableStack())

    return OtOperation(acc, o1.origin, o1.timestamp, o1.kind)
}



data class OtTransformResult(val newLocalDiff: OtOperation, val localizedApplyToDocument: OtOperation)


// Ressel’s transformation function
fun transform(localDiff: OtOperation, remoteApplyToDocument: OtOperation): OtTransformResult {

    require(localDiff.documentLengthBefore() == remoteApplyToDocument.documentLengthBefore())
    require(localDiff.origin != remoteApplyToDocument.origin)
    require(localDiff.kind == OtOperationKind.Normal && remoteApplyToDocument.kind == OtOperationKind.Normal)

    tailrec fun transform0(resOp1: MutableList<OtChange>, resOp2: MutableList<OtChange>, ops1: ImmutableStack<OtChange>, ops2: ImmutableStack<OtChange>) {
        val op1: OtChange? = ops1.peek()
        val op2: OtChange? = ops2.peek()

        if (op1 == null && op2 == null) return

        val tail1 = ops1.tail()
        val tail2 = ops2.tail()

        when {
            op1 == null && op2 != null -> {
                val offset = op2.getTextLengthAfter()
                if (offset > 0)
                    transform0(resOp1.append(Retain(offset)), resOp2.append(op2), tail1, tail2)
                else
                    transform0(resOp1, resOp2.append(op2), tail1, tail2)
            }
            op1 != null && op2 == null -> {
                val offset = op1.getTextLengthAfter()
                if (offset > 0)
                    transform0(resOp1.append(op1), resOp2.append(Retain(offset)), tail1, tail2)
                else
                    transform0(resOp1.append(op1), resOp2, tail1, tail2)
            }
            op1 is InsertText && op2 is InsertText -> {
                if (localDiff.origin < remoteApplyToDocument.origin)
                    transform0(resOp1.append(op1), resOp2.append(Retain(op1.text.length)), tail1, ops2)
                else
                    transform0(resOp1.append(Retain(op2.text.length)), resOp2.append(op2), ops1, tail2)
            }
            op1 is InsertText -> transform0(resOp1.append(op1), resOp2.append(Retain(op1.text.length)), tail1, ops2)
            op2 is InsertText -> transform0(resOp1.append(Retain(op2.text.length)), resOp2.append(op2), ops1, tail2)
            op1 is Retain && op2 is Retain -> {
                val offset1 = op1.offset
                val offset2 = op2.offset
                when {
                    offset1 > offset2 -> transform0(resOp1.append(op2), resOp2.append(op2), tail1.push(Retain(offset1 - offset2)), tail2)
                    offset1 == offset2 -> transform0(resOp1.append(op1), resOp2.append(op1), tail1, tail2)
                    offset1 < offset2 -> transform0(resOp1.append(op1), resOp2.append(op1), tail1, tail2.push(Retain(offset2 - offset1)))
                }
            }
            op1 is DeleteText && op2 is DeleteText -> {
                val text1 = op1.text
                val text2 = op2.text
                when {
                    text1.length > text2.length -> transform0(resOp1, resOp2, tail1.push(DeleteText(text1.substring(text2.length))), tail2)
                    text1.length == text2.length -> {
                        require(text1 == text2)
                        transform0(resOp1, resOp2, tail1, tail2)
                    }
                    text1.length < text2.length -> transform0(resOp1, resOp2, tail1, tail2.push(DeleteText(text2.substring(text1.length))))
                }
            }
            op1 is DeleteText && op2 is Retain -> {
                val text1 = op1.text
                val offset2 = op2.offset
                when {
                    text1.length > offset2 -> transform0(resOp1.append(DeleteText(text1.substring(0, offset2))), resOp2,
                            tail1.push(DeleteText(text1.substring(offset2))), tail2)
                    text1.length == offset2 -> transform0(resOp1.append(op1), resOp2, tail1, tail2)
                    text1.length < offset2 -> transform0(resOp1.append(op1), resOp2, tail1, tail2.push(Retain(offset2 - text1.length)))
                }
            }
            op1 is Retain && op2 is DeleteText -> {
                val offset1 = op1.offset
                val text2 = op2.text
                when {
                    offset1 > text2.length -> transform0(resOp1, resOp2.append(op2), tail1.push(Retain(offset1 - text2.length)), tail2)
                    offset1 == text2.length -> transform0(resOp1, resOp2.append(op2), tail1, tail2)
                    offset1 < text2.length -> transform0(resOp1, resOp2.append(DeleteText(text2.substring(0, offset1))),
                            tail1, tail2.push(DeleteText(text2.substring(offset1))))
                }
            }
        }
    }

    val newLocalDiff = ArrayList<OtChange>()
    val localizedApplyToDocument = ArrayList<OtChange>()
    transform0(newLocalDiff, localizedApplyToDocument, localDiff.changes.toImmutableStack(), remoteApplyToDocument.changes.toImmutableStack())

    return OtTransformResult(
            OtOperation(newLocalDiff, localDiff.origin, localDiff.timestamp, OtOperationKind.Normal),
            OtOperation(localizedApplyToDocument, remoteApplyToDocument.origin, remoteApplyToDocument.timestamp, OtOperationKind.Normal)
    )
}
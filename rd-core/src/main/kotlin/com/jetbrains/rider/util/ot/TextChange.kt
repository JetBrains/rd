package com.jetbrains.rider.util.ot

data class TextChange(val offset: Int, val old: String, val new: String, val fullDocumentLength: Int/*after applying change*/)

fun TextChange.toOperation(role: OtRole): OtOperation {
    val changes = mutableListOf<OtChange>().apply {
        add(Retain(offset))
        if (!new.isEmpty()) add(InsertText(new))
        if (!old.isEmpty()) add(DeleteText(old))
        val currentOffset = this.sumBy(OtChange::getTextLengthAfter)
        add(Retain(fullDocumentLength - currentOffset))
    }
    val operation = OtOperation(changes, role)

    require(operation.documentLengthAfter() == fullDocumentLength)
    return operation
}
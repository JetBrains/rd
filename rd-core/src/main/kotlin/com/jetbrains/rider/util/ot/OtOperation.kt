package com.jetbrains.rider.util.ot

class OtOperation(changes: List<OtChange>, val role: OtRole) {
    val changes = normalize(changes)

    private fun normalize(changes: List<OtChange>): List<OtChange> {
        val result = arrayListOf<OtChange>()
        var prev: OtChange? = null
        for (curr in changes) {
            if (curr.isId()) continue

            prev = when {
                prev is Retain && curr is Retain -> Retain(prev.offset + curr.offset)
                prev is InsertText &&  curr is InsertText -> InsertText(prev.text + curr.text)
                prev is DeleteText && curr is DeleteText -> DeleteText(prev.text + curr.text)
                else -> {
                    if (prev != null) result.add(prev)
                    curr
                }
            }
        }
        if (prev != null) result.add(prev)

        if (result.size == 1 && result[0] is Retain)
            return emptyList()

        return result
    }

    fun documentLengthBefore() = changes.sumBy(OtChange::getTextLengthBefore)
    fun documentLengthAfter() = changes.sumBy(OtChange::getTextLengthAfter)


    fun isIdentity(): Boolean = changes.isEmpty()

    fun invert() = OtOperation(changes.map {-it}, role)

    override fun toString(): String = changes.joinToString(";", "Op(changes=[", "], role=$role)")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as OtOperation

        if (role != other.role) return false
        if (changes != other.changes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = role.hashCode()
        result = 31 * result + changes.hashCode()
        return result
    }
}

fun OtOperation.toTextChanges(): List<TextChange> {
    if (this.isIdentity()) return emptyList()

    var documentLength = this.documentLengthBefore()
    var currOffset = 0
    var lastInsert: InsertText? = null
    var lastDelete: DeleteText? = null

    val res = mutableListOf<TextChange>()
    for (change in this.changes) {
        when (change) {
            is Retain -> {
                if (lastDelete != null || lastInsert != null) {
                    res.add(createTextChange(currOffset, lastInsert, lastDelete, documentLength))
                    lastDelete = null
                    lastInsert = null
                }
            }
            is InsertText -> {
                if (lastInsert != null) {
                    res.add(createTextChange(currOffset, lastInsert, lastDelete, documentLength))
                    lastDelete = null
                }
                documentLength += change.getTextLengthAfter()
                lastInsert = change
            }
            is DeleteText -> {
                if (lastDelete != null) {
                    res.add(createTextChange(currOffset, lastInsert, lastDelete, documentLength))
                    lastInsert = null
                }
                documentLength -= change.getTextLengthBefore()
                lastDelete = change
            }
        }

        currOffset += change.getTextLengthAfter()
    }

    if (lastDelete != null || lastInsert != null)
        res.add(createTextChange(currOffset, lastInsert, lastDelete, documentLength))
    return res
}

private fun createTextChange(offset: Int, insert: InsertText?, delete: DeleteText?, documentLength: Int): TextChange {
    val newText = insert?.text ?: ""
    val oldText = delete?.text ?: ""
    val startOffset = offset - (insert?.getTextLengthAfter() ?: 0)
    return TextChange(startOffset, oldText, newText, documentLength)
}
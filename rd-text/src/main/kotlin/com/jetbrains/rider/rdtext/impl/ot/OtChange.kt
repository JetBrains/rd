package com.jetbrains.rider.rdtext.impl.ot

/**
 * Defines tie-breaking rules (if we have unordered changes at the same offset).
 * For example, a closing brace must wrap user typing, that's why InsertText('}') must have AfterAllChanges priority,
 * but an indentation must have BeforeAllChanges priority.
 */
enum class OtChangePriority {
    BeforeAllChanges,
    Normal,
    AfterAllChanges
}

sealed class OtChange {
    abstract fun isId(): Boolean
    abstract operator fun unaryMinus() : OtChange
    abstract fun getTextLengthBefore(): Int
    abstract fun getTextLengthAfter(): Int
}

data class Retain(val offset: Int) : OtChange() {
    init { require(offset >= 0) }
    override fun isId() = offset == 0
    override fun unaryMinus() = this
    override fun getTextLengthBefore(): Int = offset
    override fun getTextLengthAfter(): Int = offset
}
data class InsertText(val text: String, val priority: OtChangePriority) : OtChange() {
    override fun isId() = text.isEmpty()
    override fun unaryMinus() = DeleteText(text, priority)
    override fun getTextLengthBefore(): Int = 0
    override fun getTextLengthAfter(): Int = text.length
}
data class DeleteText(val text: String, val priority: OtChangePriority) : OtChange() {
    override fun isId() = text.isEmpty()
    override fun unaryMinus() = InsertText(text, priority)
    override fun getTextLengthBefore(): Int = text.length
    override fun getTextLengthAfter(): Int = 0
}

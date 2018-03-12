package com.jetbrains.rider.util.ot

sealed class OtChange() {
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
data class InsertText(val text: String) : OtChange() {
    override fun isId() = text.isEmpty()
    override fun unaryMinus() = DeleteText(text)
    override fun getTextLengthBefore(): Int = 0
    override fun getTextLengthAfter(): Int = text.length
}
data class DeleteText(val text: String) : OtChange() {
    override fun isId() = text.isEmpty()
    override fun unaryMinus() = InsertText(text)
    override fun getTextLengthBefore(): Int = text.length
    override fun getTextLengthAfter(): Int = 0
}

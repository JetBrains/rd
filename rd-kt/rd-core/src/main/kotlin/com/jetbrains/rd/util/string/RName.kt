package com.jetbrains.rd.util.string

/**
 * Recursive name. For constructs like Aaaa.Bbb::CCC
 */
class RName private constructor(val parent:RName?, val localName: String, val separator: String) {

    companion object {
        val Empty = RName(null, "", "")
    }

    constructor(localName: String) : this(Empty, localName, "")

    /**
     * Separator doesn't count if localName is empty or parent is empty.
     */
    fun sub(localName: String, separator: String) = RName(this, localName, separator)

    fun getNonEmptyRoot(): RName {
        if (parent == null || parent == Empty)
            return this
        
        return parent.getNonEmptyRoot()
    }
    
    fun dropNonEmptyRoot(): RName {
        if (parent == null || parent == Empty)
            return Empty
        
        val tail = parent.dropNonEmptyRoot()
        return tail.sub(localName, separator)
    }
    
    override fun toString(): String {
        return parent?.toString()?.let {
            if (it.isEmpty())
                localName
            else
                it + separator + localName
        } ?: localName
    }


}
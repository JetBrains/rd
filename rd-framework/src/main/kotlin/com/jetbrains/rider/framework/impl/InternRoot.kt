package com.jetbrains.rider.framework.impl

import com.jetbrains.rider.framework.IInternRoot

class InternRoot(val isMaster: Boolean): IInternRoot {
    private val myItemsList = ArrayList<Any>()
    private val otherItemsList = ArrayList<Any>()
    private val inverseMap = HashMap<Any, Int>()

    override fun tryGetInterned(value: Any): Int {
        return inverseMap[value] ?: -1
    }

    override fun internValue(value: Any): Int {
        return inverseMap[value] ?: run {
            val idx = myItemsList.size * 2 + if(isMaster) 1 else 0
            myItemsList.add(value)
            inverseMap[value] = idx
            idx
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unInternValue(id: Int): T {
        return (if (isIndexOwned(id)) myItemsList else otherItemsList)[id / 2] as T
    }

    private fun isIndexOwned(id: Int) = (id and 1 == 0) xor isMaster

    override fun setInternedCorrespondence(id: Int, value: Any) {
        require(!isIndexOwned(id), { "Setting interned correspondence for object that we should have written, bug?" })
        require(id / 2 == otherItemsList.size, { "Out-of-sequence interned object id" })

        otherItemsList.add(value)
        inverseMap[value] = id
    }
}
package com.jetbrains.rd.framework.impl

import com.jetbrains.rd.util.addUnique
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.jetbrains.rd.util.reactive.AddRemove
import com.jetbrains.rd.util.reactive.IMutableViewableSet
import com.jetbrains.rd.util.reactive.ISource
import com.jetbrains.rd.util.reactive.IViewableSet

class SwitchingViewableSet<T : Any>(lifetime: Lifetime, private var myBackingSet: IMutableViewableSet<T>): IMutableViewableSet<T> {

    private val myListeners = LinkedHashSet<(IViewableSet.Event<T>) -> Unit>()
    private val myAdviseSeqLifetimes = SequentialLifetimes(lifetime)

    override val change: ISource<IViewableSet.Event<T>>
        get() = this

    init {
        adviseForBackingSet()
    }

    private fun adviseForBackingSet() {
        val adviseLt = myAdviseSeqLifetimes.next()
        var initial = true
        myBackingSet.advise(adviseLt) { event ->
            if (!initial) myListeners.forEach { it(event) }
        }
        initial = false
    }

    override fun advise(lifetime: Lifetime, handler: (IViewableSet.Event<T>) -> Unit) {
        myBackingSet.forEach { handler(IViewableSet.Event(AddRemove.Add, it)) }
        myListeners.addUnique(lifetime, handler)
    }

    fun changeBackingSet(newBackingSet: IMutableViewableSet<T>, isNewSetMaster: Boolean = true) {
        if (isNewSetMaster) {
            val missingValues = myBackingSet - newBackingSet
            val newValue = newBackingSet - myBackingSet
            missingValues.forEach {
                myListeners.forEach { listener ->
                    listener(IViewableSet.Event(AddRemove.Remove, it))
                }
            }
            newValue.forEach {
                myListeners.forEach { listener ->
                    listener(IViewableSet.Event(AddRemove.Add, it))
                }
            }
        }
        val oldBackingSet = myBackingSet
        myBackingSet = newBackingSet
        if(!isNewSetMaster) {
            newBackingSet.retainAll(oldBackingSet)
            newBackingSet.addAll(oldBackingSet)
        }
        adviseForBackingSet()
    }


    override fun add(element: T) = myBackingSet.add(element)
    override fun addAll(elements: Collection<T>) = myBackingSet.addAll(elements)
    override fun clear() = myBackingSet.clear()
    override fun iterator() = myBackingSet.iterator()
    override fun remove(element: T) = myBackingSet.remove(element)
    override fun removeAll(elements: Collection<T>) = myBackingSet.removeAll(elements)
    override fun retainAll(elements: Collection<T>) = myBackingSet.removeAll(elements)
    override val size: Int
        get() = myBackingSet.size
    override fun contains(element: T) = myBackingSet.contains(element)
    override fun containsAll(elements: Collection<T>) = myBackingSet.containsAll(elements)
    override fun isEmpty() = myBackingSet.isEmpty()
}
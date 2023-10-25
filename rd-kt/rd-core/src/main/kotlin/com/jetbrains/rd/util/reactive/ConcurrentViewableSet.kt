package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.lifetime.*

interface IViewableConcurrentSet<T> : Iterable<T> {
    val size: Int

    fun contains(value: T): Boolean
    fun view(lifetime: Lifetime, action: (Lifetime, T) -> Unit)
}

interface IAppendOnlyViewableConcurrentSet<T> : IViewableConcurrentSet<T> {
    fun add(value: T): Boolean

    /**
     * Adds all the elements of the specified collection to this collection.
     *
     * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
     */
    fun addAll(elements: Iterable<T>): Boolean {
        var added = false
        for (element in elements) {
            if (add(element))
                added = true
        }

        return added
    }
}

interface IMutableViewableConcurrentSet<T> : IAppendOnlyViewableConcurrentSet<T> {
    fun remove(value: T): Boolean
}

class ConcurrentViewableSet<T> : IMutableViewableConcurrentSet<T> {
    private val signal = Signal<VersionedData<T>>()
    private var map = LinkedHashMap<T, LifetimeDefinition>()
    private val locker = Any()

    @Volatile
    override var size = 0
        private set

    private var addVersion = 0
    private var isUnderReadingCount = 0


    override fun add(value: T): Boolean {
        var definition: LifetimeDefinition
        var version: Int

        synchronized(locker) {
            val map = getOrCloneMapNoLock()
            val prevDefinition = map[value]
            if (prevDefinition != null && prevDefinition.lifetime.isAlive)
                return false

            definition = LifetimeDefinition()
            map[value] = definition
            version = ++addVersion
            size++
        }

        signal.fire(VersionedData(definition.lifetime, value, version))
        return true
    }

    override fun remove(value: T): Boolean {
        val definitionToRemove: LifetimeDefinition
        synchronized(locker) {
            val map = getOrCloneMapNoLock()
            definitionToRemove = map[value] ?: return false
        }

        definitionToRemove.terminate()

        synchronized(locker) {
            val map = getOrCloneMapNoLock()
            val definition = map[value] ?: return false
            if (definition != definitionToRemove)
                return false

            map.remove(value)
            size--
        }

        return true
    }

    override fun contains(value: T): Boolean {
        return tryGetLifetime(value)?.isAlive ?: false
    }

    private fun tryGetLifetime(value: T): Lifetime? {
        synchronized(locker) {
            return map[value]?.lifetime
        }
    }

    override fun view(lifetime: Lifetime, action: (Lifetime, T) -> Unit) {
        val localMap: LinkedHashMap<T, LifetimeDefinition>
        synchronized(locker) {
            localMap = map
            val version = addVersion

            signal.advise(lifetime) { versionedData ->
                if (versionedData.version <= version)
                    return@advise

                val newLifetime = versionedData.lifetime.intersect(lifetime)
                if (newLifetime.isNotAlive)
                    return@advise

                action(newLifetime, versionedData.value)
            }

            if (localMap.size == 0)
                return

            isUnderReadingCount++
        }

        for ((value, definition) in localMap) {
            val newLifetime = definition.lifetime.intersect(lifetime)
            if (newLifetime.isNotAlive)
                continue

            try {
                action(newLifetime, value)
            } catch (e: Throwable) {
                Logger.root.error(e)
            }
        }

        synchronized(locker) {
            if (map === localMap) {
                val count = isUnderReadingCount--
                assert(count >= 0)
            }
        }
    }

    override fun iterator(): Iterator<T> {
        return iterator {
            val localMap = synchronized(locker) {
                isUnderReadingCount++
                map
            }
            try {
                for (entry in localMap) {
                    yield(entry.key)
                }
            } finally {
                synchronized(locker) {
                    if (localMap === map) {
                        val count = isUnderReadingCount--
                        assert(count >= 0)
                    }
                }
            }
        }
    }

    private fun getOrCloneMapNoLock(): LinkedHashMap<T, LifetimeDefinition> {
        var localMap = map
        if (isUnderReadingCount > 0) {
            localMap = LinkedHashMap(localMap)
            isUnderReadingCount = 0
            map = localMap
            return localMap
        }

        return localMap
    }

    private class VersionedData<T>(val lifetime: Lifetime, val value: T, val version: Int)
}
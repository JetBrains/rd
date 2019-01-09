package com.jetbrains.rd.util.threading

import com.jetbrains.rd.util.Sync

// Puts linearization points into program starting with 0.
class Linearization {

    private var nextId = 0L
    private val lock = Any()


    private var enabled = true

    fun enable() {
        Sync.lock(lock) {
            enabled = true
            Sync.notifyAll(lock)
        }
    }

    fun disable() {
        Sync.lock(lock) {
            enabled = false
            Sync.notifyAll(lock)
        }
    }


    fun point(id: Int) {
        require(id >= 0) {"$id >= 0"}

        Sync.lock(lock) {
            while (enabled && id > nextId)
                Sync.wait(lock, 1000L)

            if (!enabled) return

            require (id <= nextId) {"Point $id already set, nextId=$nextId"}
            nextId ++
            Sync.notifyAll(lock)
        }
    }

    fun reset() {
        Sync.lock(lock) {
            nextId = 0L
            Sync.notifyAll(lock)
        }
    }
}
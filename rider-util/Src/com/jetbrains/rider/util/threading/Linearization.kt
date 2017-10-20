package com.jetbrains.rider.util.threading

import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier

class Linearization {

    private var nextId = 0L
    private val lock = Object()


    private var enabled = true

    fun enable() {
        synchronized(lock) {
            enabled = true
            lock.notifyAll()
        }
    }

    fun disable() {
        synchronized(lock) {
            enabled = false
            lock.notifyAll()
        }
    }


    fun point(id: Int) {
        require(id >= 0) {"$id >= 0"}

        synchronized(lock) {
            while (enabled && id > nextId) lock.wait(1000L)
            if (!enabled) return

            require (id <= nextId) {"Point $id already set, nextId=$nextId"}
            nextId ++
            lock.notifyAll()
        }
    }

    fun reset() {
        synchronized(lock) {
            nextId = 0L
            lock.notifyAll()
        }
    }
}
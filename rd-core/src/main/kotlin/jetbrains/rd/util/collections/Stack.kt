package com.jetbrains.rider.util.collections

interface Stack<T> : Collection<T> {
    fun pop(): T
    fun push(value: T)
    fun peek(): T?
}
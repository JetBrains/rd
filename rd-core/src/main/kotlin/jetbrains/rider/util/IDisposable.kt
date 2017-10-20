package com.jetbrains.rider.util

/**
 * Created by Kirill.Skrygan on 9/27/2015.
 */

public interface IDisposable{
    fun dispose();
}

public class EmptyIDisposable : IDisposable{
    companion object{
        val instance = EmptyIDisposable()
    }
    override fun dispose() {
    }
}

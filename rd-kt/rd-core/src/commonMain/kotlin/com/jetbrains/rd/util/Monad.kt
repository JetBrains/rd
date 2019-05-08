package com.jetbrains.rd.util

/**
 * Classical Maybe monad
 */
sealed class Maybe<out T> {
    object None : Maybe<Nothing>()  {
        override fun equals(other: Any?) = other === None
        override fun hashCode(): Int = -1
        override fun toString(): String = "Maybe.None"
    }

    class Just<T>(val value: T) : Maybe<T>() {
        override fun equals(other: Any?) = (other as? Just<*>)?.value?.equals(value) ?: false
        override fun hashCode(): Int = value?.hashCode()?:0
        override fun toString(): String = "Maybe.Just($value)"

    }

    val hasValue: Boolean get() = this is Just

    val asNullable : T? get() = when (this) {
        is None -> null
        is Just -> value
    }

    fun orElseThrow(err: () -> IllegalStateException): T {
        when (this) {
            is None -> throw err()
            is Just -> return value
        }
    }
}

fun <T:Any> Maybe<T>.asNullable() : T? {
    return when (this) {
        is Maybe.None -> null
        is Maybe.Just -> value
    }
}

/**
 * Classical Result monad
 */
sealed class Result<out T> {
    companion object {
        inline fun <T> wrap(action: () -> T) : Result<T> {
            return try {
                Success(action())
            } catch (t : Throwable) {
                Failure(t)
            }
        }
    }
    class Success<T>(val value: T) : Result<T>()
    class Failure(val error: Throwable) : Result<Nothing>()

    inline fun<E> transform(onSuccess: (T) -> E, onFailure: (Throwable) -> E) : E {
        return when (this) {
            is Success -> onSuccess(value)
            is Failure -> onFailure(error)
        }
    }

    fun unwrap() : T {
        return when (this) {
            is Success -> value
            is Failure -> throw error
        }
    }
}
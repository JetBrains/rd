package com.jetbrains.rider.util.hash

import kotlin.experimental.and

/**
 * murmur hash 2.0.
 *
 * The murmur hash is a relatively fast hash function from
 * http://murmurhash.googlepages.com/ for platforms with efficient
 * multiplication.
 *
 * This is a re-implementation of the original C code plus some
 * additional features.
 *
 * Public domain.
 *
 * @author Viliam Holub
 * @version 1.0.2
 */
object MurmurHash {

    /**
     * Generates 64 bit hash from byte array of the given length and seed.
     *
     * @param data byte array to hash
     * @param length length of the array to hash
     * @param seed initial seed value
     * @return 64 bit hash of the given array
     */
    @JvmOverloads
    fun hash64(data: ByteArray, length: Int, seed: Int = -0x1e85eb9b): Long {
        val m = -0x395b586ca42e166bL
        val r = 47

        var h: Long = (seed.toLong() and 0xffffffffL) xor (length * m)

        val length8 = length / 8

        for (i in 0 until length8) {
            val i8 = i * 8
            var k = ((data[i8 + 0].toLong() and 0xff) + (data[i8 + 1].toLong() and 0xff shl 8)
                    + (data[i8 + 2].toLong() and 0xff shl 16) + (data[i8 + 3].toLong() and 0xff shl 24)
                    + (data[i8 + 4].toLong() and 0xff shl 32) + (data[i8 + 5].toLong() and 0xff shl 40)
                    + (data[i8 + 6].toLong() and 0xff shl 48) + (data[i8 + 7].toLong() and 0xff shl 56))

            k *= m
            k = k xor k.ushr(r)
            k *= m

            h = h xor k
            h *= m
        }
        
        val ff = 0xff.toByte()

        when (length % 8) {
            7 -> {
                h = h xor ((data[(length and 7.inv()) + 6] and ff).toLong() shl 48)
                h = h xor ((data[(length and 7.inv()) + 5] and ff).toLong() shl 40)
                h = h xor ((data[(length and 7.inv()) + 4] and ff).toLong() shl 32)
                h = h xor ((data[(length and 7.inv()) + 3] and ff).toLong() shl 24)
                h = h xor ((data[(length and 7.inv()) + 2] and ff).toLong() shl 16)
                h = h xor ((data[(length and 7.inv()) + 1] and ff).toLong() shl 8)
                h = h xor (data[length and 7.inv()] and ff).toLong()
                h *= m
            }
            6 -> {
                h = h xor ((data[(length and 7.inv()) + 5] and ff).toLong() shl 40)
                h = h xor ((data[(length and 7.inv()) + 4] and ff).toLong() shl 32)
                h = h xor ((data[(length and 7.inv()) + 3] and ff).toLong() shl 24)
                h = h xor ((data[(length and 7.inv()) + 2] and ff).toLong() shl 16)
                h = h xor ((data[(length and 7.inv()) + 1] and ff).toLong() shl 8)
                h = h xor (data[length and 7.inv()] and ff).toLong()
                h *= m
            }
            5 -> {
                h = h xor ((data[(length and 7.inv()) + 4] and ff).toLong() shl 32)
                h = h xor ((data[(length and 7.inv()) + 3] and ff).toLong() shl 24)
                h = h xor ((data[(length and 7.inv()) + 2] and ff).toLong() shl 16)
                h = h xor ((data[(length and 7.inv()) + 1] and ff).toLong() shl 8)
                h = h xor (data[length and 7.inv()] and ff).toLong()
                h *= m
            }
            4 -> {
                h = h xor ((data[(length and 7.inv()) + 3] and ff).toLong() shl 24)
                h = h xor ((data[(length and 7.inv()) + 2] and ff).toLong() shl 16)
                h = h xor ((data[(length and 7.inv()) + 1] and ff).toLong() shl 8)
                h = h xor (data[length and 7.inv()] and ff).toLong()
                h *= m
            }
            3 -> {
                h = h xor ((data[(length and 7.inv()) + 2] and ff).toLong() shl 16)
                h = h xor ((data[(length and 7.inv()) + 1] and ff).toLong() shl 8)
                h = h xor (data[length and 7.inv()] and ff).toLong()
                h *= m
            }
            2 -> {
                h = h xor ((data[(length and 7.inv()) + 1] and ff).toLong() shl 8)
                h = h xor (data[length and 7.inv()] and ff).toLong()
                h *= m
            }
            1 -> {
                h = h xor (data[length and 7.inv()] and ff).toLong()
                h *= m
            }
        }

        h = h xor h.ushr(r)
        h *= m
        h = h xor h.ushr(r)

        return h
    }

    /**
     * Generates 64 bit hash from a string.
     *
     * @param text string to hash
     * @return 64 bit hash of the given string
     */
    fun hash64(text: String, seed: Int = -0x1e85eb9b): Long {
        val bytes = text.toByteArray()
        return hash64(bytes, bytes.size, seed = seed)
    }

    /**
     * Generates 64 bit hash from a substring.
     *
     * @param text string to hash
     * @param from starting index
     * @param length length of the substring to hash
     * @return 64 bit hash of the given array
     */
    fun hash64(text: String, from: Int, length: Int): Long {
        return hash64(text.substring(from, from + length))
    }
}

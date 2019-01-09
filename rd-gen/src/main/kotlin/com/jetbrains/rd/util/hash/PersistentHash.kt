package com.jetbrains.rd.util.hash

import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

class PersistentHash {
    companion object {
        fun load(path: Path) = PersistentHash().apply {
            val file = path.toFile()
            if (!file.isFile) return PersistentHash()

            file.readLines()
                .filter { !it.isBlank() }
                .forEach { line ->
                    var builder = StringBuilder()
                    var key: String? = null
                    var i = 0
                    while (i < line.length) {
                        when (line[i]) {
                            '\\' -> {
                                i++
                                if (line[i] == 'r') builder.append('\r')
                                else if (line[i] == 'n') builder.append('\n')
                                else builder.append(line[i])
                            }
                            '=' -> {
                                key = builder.toString()
                                builder = StringBuilder()
                            }
                            ',' -> {
                                this.mix(key!!, builder.toString())
                                builder = StringBuilder()
                            }
                            else -> builder.append(line[i])
                        }
                        i++
                    }
                    this.mix(key!!, builder.toString())
                }
        }
    }

    val items = sortedMapOf<String, SortedSet<String>>()

    fun mix(key: String, value: String?) {
        if (value == null) return

        val (k, v) = key to value
        val values = items[k]?:TreeSet<String>().also {items[k] = it}
        values.add(v)
    }


    private fun encode(str : String) = StringBuilder().apply {
        for (c in str)
            append(when (c) {
                '=',',','\\' -> "\\$c"
                '\r' -> "\\r"
                '\n' -> "\\n"
                else -> "$c"
            })
    }

    fun store(file: Path) {
        val sb = StringBuilder()
        for ((k, lst) in items) {
            sb.append(encode(k))
            sb.append('=')
            sb.append(lst.joinToString(",") {encode(it)})
            sb.append(System.lineSeparator())
        }
        val toFile = file.toFile()
        toFile.parentFile.mkdirs()
        toFile.writeText(sb.toString())
    }

    operator fun get(key: String) : SortedSet<String> {
        return items[key]?: sortedSetOf()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PersistentHash

        if (items != other.items) return false

        return true
    }

    //commutative
    override fun hashCode(): Int = items.hashCode()


    fun firstDiff(other: PersistentHash) : String? {
        for ((k,v) in items) if (other[k] != v) return k
        for ((k,v) in other.items) if (this[k] != v) return k
        return null
    }

    private val format = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS")
    fun mixFile(f: File) {
        if (f.exists()) mix("file: ${f.canonicalPath}", format.format(Date(f.lastModified())))
    }

    fun mixFileRecursively(folder: File, filter: (File) -> Boolean = { it.isFile }) {
        folder.walkTopDown().filter ( filter ).forEach { mixFile(it) }
    }
}
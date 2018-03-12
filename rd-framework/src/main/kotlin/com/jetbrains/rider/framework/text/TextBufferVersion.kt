package com.jetbrains.rider.framework.text

data class TextBufferVersion(val master: Int, val slave: Int) : Comparable<TextBufferVersion> {
    companion object {
        val INIT_VERSION = TextBufferVersion(-1, -1)
    }

    fun incrementMaster() = TextBufferVersion(master + 1, slave)
    fun incrementSlave() = TextBufferVersion(master, slave + 1)

    override fun compareTo(other: TextBufferVersion): Int {
        val masterCompare = master.compareTo(other.master)
        if (masterCompare != 0) return masterCompare
        return slave.compareTo(other.slave)
    }
}
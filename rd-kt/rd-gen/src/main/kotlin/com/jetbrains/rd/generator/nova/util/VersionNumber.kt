package com.jetbrains.rd.generator.nova.util

data class VersionNumber(val major : Int, val minor : Int, val micro : Int) : Comparable<VersionNumber> {
    override fun compareTo(other: VersionNumber): Int {
        if (major != other.major) {
            return major - other.major
        }
        if (minor != other.minor) {
            return minor - other.minor
        }
        if (micro != other.micro) {
            return micro - other.micro
        }
        return 0
    }
}
package com.jetbrains.rd.framework.impl

/**
 * This is a temporary stub for proper secure strings in protocol
 * Unlike a normal string, it won't be stored in logs or any other string representations of protocol entities
 */
class RdSecureString(val contents: String) {
    override fun toString() = "RdSecureString"
    override fun equals(other: Any?) = other is RdSecureString && contents == other.contents
    override fun hashCode() = contents.hashCode()
}

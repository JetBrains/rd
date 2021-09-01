package com.jetbrains.rd.util.string

inline fun Boolean.condstr(f: () -> String) : String {
    return if (this) f() else ""
}
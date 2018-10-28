package com.jetbrains.rider.generator.nova.util

import com.jetbrains.rider.generator.nova.Enum
import com.jetbrains.rider.generator.nova.Member

fun StringBuilder.appendDefaultValueSetter(member: Member, typeName: String) {
    if (member is Member.Field && (member.isOptional || member.defaultValue != null)) {
        append(" = ")
        val defaultValue = member.defaultValue
        when (defaultValue) {
            is String -> append(if (member.type is Enum) "$typeName.$defaultValue" else "\"$defaultValue\"")
            is Long, is Boolean -> append(defaultValue)
            else -> if (member.isOptional) append("null")
        }
    }
}
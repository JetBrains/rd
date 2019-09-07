package com.jetbrains.rd.generator.nova.cpp

import com.jetbrains.rd.generator.nova.IType

data class CppIntrinsicType(override val name: String, val header: String?) : IType
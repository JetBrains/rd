package com.jetbrains.rd.generator.nova.cpp

import com.jetbrains.rd.generator.nova.Declaration
import com.jetbrains.rd.generator.nova.ITypeDeclaration
import com.jetbrains.rd.generator.nova.util.decapitalizeInvariant

data class CppIntrinsicType(val namespace: String?, override val name: String, val header: String?) : Declaration(null), ITypeDeclaration {
    override val _name: String
        get() = "${javaClass.simpleName.decapitalizeInvariant()}_cpp_intrinsic"
    override val cl_name: String
        get() = "${javaClass.simpleName.decapitalizeInvariant()}_class"
}
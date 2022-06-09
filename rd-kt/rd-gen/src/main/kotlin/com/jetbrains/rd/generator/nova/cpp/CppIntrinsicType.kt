package com.jetbrains.rd.generator.nova.cpp

import com.jetbrains.rd.generator.nova.Declaration
import com.jetbrains.rd.generator.nova.IType
import com.jetbrains.rd.generator.nova.ITypeDeclaration

data class CppIntrinsicType(val namespace: String?, override val name: String, val header: String?) : Declaration(null), ITypeDeclaration {
    override val _name: String
        get() = "${javaClass.simpleName.decapitalize()}_cpp_intrinsic"
    override val cl_name: String
        get() = "${javaClass.simpleName.decapitalize()}_class"
}
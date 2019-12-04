package com.jetbrains.rd.generator.nova.cpp

import com.jetbrains.rd.generator.nova.Declaration
import com.jetbrains.rd.generator.nova.IType

data class CppIntrinsicType(val namespace: String?, override val name: String, val header: String?) : Declaration(null), IType {
    override val _name: String
        get() = "${javaClass.simpleName.decapitalize()}_cpp_intrinsic"
    override val cl_name: String
        get() = "${javaClass.simpleName.decapitalize()}_class"
}
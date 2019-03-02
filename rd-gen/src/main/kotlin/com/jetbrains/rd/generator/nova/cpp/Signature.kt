package com.jetbrains.rd.generator.nova.cpp

import com.jetbrains.rd.generator.nova.Declaration
import com.jetbrains.rd.generator.nova.util.joinToOptString
import com.jetbrains.rd.util.string.condstr

class Signature(private val returnType: String, private val arguments: String, private val scope: String, private var isAbstract: Boolean = false) {
    private var declPrefix = arrayListOf<String>()
    private var declPostfix = arrayListOf<String>()
    private var commonPostfix = arrayListOf<String>()

    protected fun <T> ArrayList<T>.front(): String {
        return toArray().joinToOptString(separator = " ", postfix = " ")
    }

    protected fun <T> ArrayList<T>.back(): String {
        return toArray().joinToOptString(separator = " ", prefix = " ")
    }

    fun decl(): String {
        return "${declPrefix.front()}$returnType $arguments${commonPostfix.back()}${declPostfix.back()}" + isAbstract.condstr { " = 0" } + ";"
    }

    fun def(): String {
        return "$returnType $scope::$arguments${commonPostfix.back()}"
    }

    fun const(): Signature {
        return this.also {
            commonPostfix.add("const")
        }
    }

    fun override(): Signature {
        return this.also {
            declPostfix.add("override")
        }
    }

    fun static(): Signature {
        return this.also {
            declPrefix.add("static")
        }
    }

    fun abstract(decl: Declaration? = null): Signature {
        return this.also {
            isAbstract = true
            declPrefix.add("virtual")
            decl?.base?.let {
                override()
            }
        }
    }
}
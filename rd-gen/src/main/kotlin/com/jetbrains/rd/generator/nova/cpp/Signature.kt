package com.jetbrains.rd.generator.nova.cpp

import com.jetbrains.rd.generator.nova.Declaration
import com.jetbrains.rd.generator.nova.Member
import com.jetbrains.rd.generator.nova.util.joinToOptString
import com.jetbrains.rd.util.eol
import com.jetbrains.rd.util.string.condstr


sealed class Signature {
    abstract fun decl() : String
    abstract fun def() : String


    class MemberFunction(private val returnType: String, private val arguments: String, private val scope: String, private var isAbstract: Boolean = false) : Signature() {
        private var declPrefix = arrayListOf<String>()
        private var declPostfix = arrayListOf<String>()
        private var commonPostfix = arrayListOf<String>()

        private fun <T> ArrayList<T>.front(): String {
            return toArray().joinToOptString(separator = " ", postfix = " ")
        }

        private fun <T> ArrayList<T>.back(): String {
            return toArray().joinToOptString(separator = " ", prefix = " ")
        }

        override fun decl(): String {
            return "${declPrefix.front()}$returnType $arguments${commonPostfix.back()}${declPostfix.back()}" + isAbstract.condstr { " = 0" } + ";"
        }

        override fun def(): String {
            return "$returnType $scope::$arguments${commonPostfix.back()}"
        }

        fun const(): MemberFunction {
            return this.also {
                commonPostfix.add("const")
            }
        }

        fun override(): MemberFunction {
            return this.also {
                declPostfix.add("override")
            }
        }

        fun static(): MemberFunction {
            return this.also {
                declPrefix.add("static")
            }
        }

        fun abstract(decl: Declaration? = null): MemberFunction {
            return this.also {
                isAbstract = true
                declPrefix.add("virtual")
                decl?.base?.let {
                    override()
                }
            }
        }
    }

    class Constructor(val generator: Cpp17Generator, private val decl: Declaration, private val arguments: List<Member>) : Signature() {
        private var isExplicit = false

        override fun decl(): String {
            val params = arguments.map { generator.ctorParam(it, decl, false) }.joinToString(separator = ", ")
            return isExplicit.condstr { "explicit " } + "${decl.name}($params);"
        }

        override fun def(): String {
            val params = arguments.map { generator.ctorParam(it, decl, false) }.joinToString(separator = ", ")
            val init = arguments.map { "${it.name}_(std::move(${it.name}_))" }.joinToString(separator = ", ")
            val name = decl.name
            return "$name::$name($params):$init"
        }

        fun explicit(): Constructor? {
            return this.also {
                isExplicit = true
            }
        }
    }
}
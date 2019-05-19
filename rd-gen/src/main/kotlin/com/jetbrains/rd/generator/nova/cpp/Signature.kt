package com.jetbrains.rd.generator.nova.cpp

import com.jetbrains.rd.generator.nova.Declaration
import com.jetbrains.rd.generator.nova.IType
import com.jetbrains.rd.generator.nova.Member
import com.jetbrains.rd.generator.nova.util.joinToOptString
import com.jetbrains.rd.util.eol
import com.jetbrains.rd.util.string.condstr


sealed class Signature {
    abstract fun decl(): String
    abstract fun def(): String


    class MemberFunction(private val returnType: String, private val arguments: String, private val scope: String?, private var isAbstract: Boolean = false) : Signature() {
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
            return "$returnType ${scope?.let { "$it::" }.orEmpty()}$arguments${commonPostfix.back()}"
        }

        fun const(): MemberFunction {
            return this.also {
                commonPostfix.add("const")
            }
        }

        fun noexcept(): MemberFunction {
            return this.also {
                commonPostfix.add("noexcept")
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

        fun friend(): MemberFunction {
            return this.also {
                declPrefix.add("friend")
            }
        }
    }

    sealed class Constructor(protected val generator: Cpp17Generator, protected val decl: Declaration, private val arguments: List<Member>) : Signature() {
        private var isExplicit = false

        val name = decl.name
        val params = arguments.joinToString(separator = ", ") { generator.ctorParam(it, decl, false) }

        override fun decl(): String {
            return isExplicit.condstr { "explicit " } + "${decl.name}($params);"
        }

        class Default(generator: Cpp17Generator, decl: Declaration) : Constructor(generator, decl, emptyList()) {
            override fun def(): String {
                return "$name::$name()"
            }
        }

        class Primary(generator: Cpp17Generator, decl: Declaration, private val allArguments: AllArguments) : Constructor(generator, decl, allArguments.allArguments) {
            class AllArguments(internal val ownArguments: List<Member> = emptyList(), private val otherArguments : List<Member> = emptyList()) {
                val allArguments = ownArguments + otherArguments

                fun isEmpty(): Boolean {
                    return ownArguments.isEmpty() && otherArguments.isEmpty()
                }
            }

            override fun def(): String {
                val initBases = generator.bases(decl).joinToString(separator = ", ") {
                    val params = it.params.joinToString(separator = ",") { p -> "std::move(${p.name}_)" }
                    "${it.type.name}($params)"
                }
                val init = allArguments.ownArguments.let {
                    if (it.isEmpty()) {
                        ""
                    } else {
                        it.joinToString(separator = ", ", prefix = ",") { p -> "${p.name}_(std::move(${p.name}_))" }
                    }
                }
                return "$name::$name($params) :$eol$initBases$eol$init"
            }
        }

        class Secondary(generator: Cpp17Generator, decl: Declaration, private val allArguments: AllArguments) : Constructor(generator, decl, allArguments.ownArguments) {
            class AllArguments(internal val ownArguments: List<Member> = emptyList(), internal val otherArguments : List<Member?> = emptyList()) {
                val allArguments = ownArguments + otherArguments

                fun isEmpty(): Boolean {
                    return ownArguments.isEmpty() && otherArguments.isEmpty()
                }
            }

            override fun def(): String {
                val init = allArguments.otherArguments.map {
                    if (it == null) {
                        "{}"
                    } else {
                        "(std::move(${it.name}_))"
                    }
                }.joinToString(separator = ",")
                return "$name::$name($params) : $eol$name($init)"
            }
        }

    }
}

class BaseClass(val type: IType, val params: List<Member>)
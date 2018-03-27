package com.jetbrains.rider.generator.nova

import java.io.File

interface IGenerator {
    val folder: File
    fun generate(root: Root, clearFolderIfExists: Boolean = false, toplevels: List<Toplevel>)
}

class GeneratorException (msg: String) : RuntimeException(msg)

abstract class GeneratorBase : IGenerator {
    protected fun fail(msg: String) : Nothing { throw GeneratorException(msg) }


    protected fun prepareGenerationFolder(folder: File, removeIfExists: Boolean) {
        //safety net to avoid 'c:\' removal or spoiling by occasion
        if (folder.toPath().nameCount == 0)
            fail("Can't use root folder '$folder' as output")


        if (removeIfExists && folder.exists() && !folder.deleteRecursively())
            fail("Can't delete '$folder'")


        if (folder.exists()) {
            if (!folder.isDirectory) fail("Not a folder: '$folder'")
        }
        else if (!folder.mkdirs())
            fail("Can't create folder '$folder'")
    }
}

val IType.hasEmptyConstructor : Boolean get() = when (this) {
    is Class.Concrete -> allMembers.all { it.hasEmptyConstructor }
    is Aggregate -> true

    else -> false
}

val Member.hasEmptyConstructor : Boolean get() = when (this) {
    is Member.Field -> type.hasEmptyConstructor && !emptyCtorSuppressed
    is Member.Reactive -> true

    else -> throw GeneratorException("Unsupported member: $this")
}

val Declaration.isConcrete
    get() = this is Class.Concrete || this is Struct.Concrete || this is Aggregate






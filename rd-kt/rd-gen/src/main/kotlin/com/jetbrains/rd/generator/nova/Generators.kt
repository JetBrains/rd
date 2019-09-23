package com.jetbrains.rd.generator.nova

import com.jetbrains.rd.generator.nova.util.compareLists
import com.jetbrains.rd.util.string.PrettyPrinter
import java.io.File


interface IGenerator {
    val flowTransform: FlowTransform
    val folders: List<File>
    val canonicalPaths: List<String>
    fun generate(root: Root, clearFolderIfExists: Boolean = false, toplevels: List<Toplevel>)
}

/**
 * Generator and root together
 */
interface IGenerationUnit : Comparable<IGenerationUnit> {
    operator fun component1(): IGenerator = generator
    operator fun component2(): Root = root

    val generator: IGenerator
    val root: Root

    //Need to sort generators because we plan to purge generation folders sometimes
    override fun compareTo(other: IGenerationUnit): Int {
        compareLists(generator.canonicalPaths, other.generator.canonicalPaths).let { if (it != 0) return it }
        root.name.compareTo(other.root.name).let { if (it != 0) return it }
        generator.javaClass.name.compareTo(other.generator.javaClass.name).let { if (it != 0) return it }

        return 0 //sort is stable so, don't worry much
    }
}

/**
 * If you extend this class with object instance it will be collected by [collectSortedGeneratorsToInvoke] during reflection search.
 */
open class GenerationUnit(override val generator: IGenerator, override val root: Root) : IGenerationUnit


/**
 * This exception arises during generation. Usually thrown by [GeneratorBase.fail] method.
 */
class GeneratorException(msg: String) : RuntimeException(msg)

/**
 * Base class for generators to deduplicate common logic
 */
abstract class GeneratorBase(final override val folders: List<File>) : IGenerator {
    override val canonicalPaths: List<String> = folders.map { it.canonicalPath }.sorted()

    protected fun fail(msg: String): Nothing {
        throw GeneratorException(msg)
    }

    protected fun prepareGenerationFolders(folders: List<File>, clearFolderIfExists: Boolean) {
        folders.forEach {
            prepareGenerationFolder(it, clearFolderIfExists)
        }
    }

    private fun prepareGenerationFolder(folder: File, removeIfExists: Boolean) {
        //safety net to avoid 'c:\' removal or spoiling by occasion
        if (folder.toPath().nameCount == 0)
            fail("Can't use root folder '$folder' as output")


        if (removeIfExists && folder.exists() && !retry { folder.deleteRecursively() }
                && /* if delete failed (held by external process) but directory cleared it's ok */ !folder.list().isNullOrEmpty()) {
            fail("Can't clear '$folder'")
        }


        if (folder.exists()) {
            if (!folder.isDirectory) fail("Not a folder: '$folder'")
        } else if (!retry { folder.mkdirs() })
            fail("Can't create folder '$folder'")
    }


    private inline fun retry(action: () -> Boolean): Boolean {
        if (action()) return true

        Thread.sleep(100)

        return action()
    }


    protected open fun unknowns(declaredTypes: Iterable<Declaration>): Collection<Declaration> {
        return declaredTypes.mapNotNull {
            val unknown: Declaration? = unknown(it)
            unknown
        }
    }

    protected open fun unknown(it: Declaration): Declaration? {
        return when (it) {
            is Struct.Abstract -> Struct.Concrete("${it.name}_Unknown", it.pointcut, it, true)
            is Class.Abstract -> Class.Concrete("${it.name}_Unknown", it.pointcut, it, true)
            else -> null
        }
    }

    protected val master get() = flowTransform != FlowTransform.Reversed

    protected fun File.writeContent(printer: PrettyPrinter.() -> Unit) {
        bufferedWriter().use { writer ->
            PrettyPrinter().apply(printer).toString().let(writer::write)
        }
    }
}






package com.jetbrains.rd.generator.nova

import java.io.File


interface IGenerator {
    val folder: File
    fun generate(toplevels: List<Toplevel>)
}

/**
 * Generator and root together
 */
interface IGeneratorAndRoot: Comparable<IGeneratorAndRoot> {
    operator fun component1(): IGenerator = generator
    operator fun component2(): Root = root

    val generator: IGenerator
    val root: Root

    //Need to sort generators because we plan to purge generation folders sometimes
    override fun compareTo(other: IGeneratorAndRoot): Int {
        generator.folder.canonicalPath.compareTo(other.generator.folder.canonicalPath).let { if (it != 0) return it}
        root.name.compareTo(other.root.name).let { if (it != 0) return it }
        generator.javaClass.name.compareTo(other.generator.javaClass.name).let { if (it != 0) return it }

        return 0 //sort is stable so, don't worry much
    }
}

/**
 * If you extend this class with object instance it will be collected by [collectSortedGeneratorsToInvoke] during reflection search.
 */
open class ExternalGenerator(override val generator: IGenerator, override val root: Root) : IGeneratorAndRoot


/**
 * This exception arises during generation. Usually thrown by [GeneratorBase.fail] method.
 */
class GeneratorException (msg: String) : RuntimeException(msg)

fun fail(msg: String) : Nothing { throw GeneratorException(msg) }

/**
 * Base class for generators to deduplicate common logic
 */
abstract class GeneratorBase(protected open val flowTransform: FlowTransform, protected val generatedFileSuffix: String) : IGenerator {
    object AllowDeconstruct: ISetting<Unit, Declaration>

    /**
     * Allows to filter out some generators for toplevel
     */
    object AcceptsGenerator: ISetting<(IGenerator) -> Boolean, Toplevel>


    protected abstract fun realGenerate(toplevels: List<Toplevel>)

    override fun generate(toplevels: List<Toplevel>) {
        val preparedToplevels = toplevels
            .filter { it.getSetting(AcceptsGenerator)?.invoke(this) ?: true }
            .sortedBy { it.name }

        realGenerate(preparedToplevels)
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
            is Struct.Open -> Struct.Concrete("${it.name}_Unknown", it.pointcut, it, true)
            is Class.Abstract -> Class.Concrete("${it.name}_Unknown", it.pointcut, it, true)
            is Class.Open -> Class.Concrete("${it.name}_Unknown", it.pointcut, it, true)
            else -> null
        }
    }

    //@Deprecated
    protected val master get() = flowTransform != FlowTransform.Reversed

    protected val Declaration.isDataClass: Boolean
        get() = this is Struct.Concrete && base == null && allMembers.isNotEmpty()
}






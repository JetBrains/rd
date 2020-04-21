package com.jetbrains.rd.generator.nova

import com.jetbrains.rd.generator.gradle.GradleGenerationSpec
import com.jetbrains.rd.generator.nova.util.InvalidSysproperty
import com.jetbrains.rd.util.reflection.scanForClasses
import com.jetbrains.rd.util.reflection.usingValue
import com.jetbrains.rd.util.string.condstr
import java.io.File
import java.lang.Class
import java.lang.reflect.Modifier

fun generateRdModel(
    classLoader: ClassLoader,
    namespacePrefixes: Array<String>,
    verbose: Boolean = false,
    generatorsFilter: Regex? = null,
    clearOutputFolderIfExists: Boolean = false,
    gradleGenerationSpecs: List<GradleGenerationSpec> = emptyList()
): Set<File> {
    val startTime = System.currentTimeMillis()

    val genfilter = generatorsFilter ?: Regex(".*")

    if (verbose) {
        println()
        println("RdGen model generator started")
        println("Searching classes with namespace prefixes: '${namespacePrefixes.joinToString(", ")}'")
        println("Regex for filtering generators: '${genfilter.pattern}'")

        if (gradleGenerationSpecs.isNotEmpty())
            println("Found ${gradleGenerationSpecs.size} gradle generators")
    }


    val javaClasses = collectClasses(classLoader, namespacePrefixes, verbose)

    val toplevels = collectTopLevels(javaClasses, verbose)


    val validationErrors = ArrayList<String>()

    val roots = toplevels.map { it.root }.distinct()
    roots.forEach { root ->
        root.initialize()
        root.validate(validationErrors)
    }


    if (validationErrors.isNotEmpty())
        throw GeneratorException("Model validation fail:" +
            validationErrors.joinToString ("") {"\n\n>> $it"}
        )

    val generatorsToInvoke = collectSortedGeneratorsToInvoke(roots, javaClasses, genfilter, verbose, gradleGenerationSpecs)

    if (verbose) {
        println()
        println("After filtering ${generatorsToInvoke.size} generator(s) to run")
    }

    val generatedFolders = HashSet<File>()
    for ((gen, root) in generatorsToInvoke) {
        val shouldClear = clearOutputFolderIfExists &&
            //whether we cleared this folder before or not?
            generatorsToInvoke
                .asSequence()
                .map { it.generator }
                .takeWhile { it != gen }
                .map {it.folder.canonicalPath}
                .none { gen.folder.canonicalPath.run { this == it || this.startsWith(it + File.separator)} }

        //Here is the real part
        if (verbose)
            println("Invoke $gen on $root, clearFolder=$shouldClear")

        ::settingCtx.usingValue(gen) {
            prepareGenerationFolder(gen.folder, shouldClear)

///         -----------------------
///             ACTUAL GENERATE
///         ----------------------
            gen.generate(toplevels.filter { it.root == root })


        }

        generatedFolders.add(gen.folder)
    }

    val endTime = System.currentTimeMillis()
    if (verbose) println("Generation finished in ${endTime - startTime} ms")

    return generatedFolders
}


fun prepareGenerationFolder(folder: File, removeIfExists: Boolean) {
    fun retry(action: () -> Boolean) : Boolean {
        if (action()) return true

        Thread.sleep(100)

        return action()
    }

    //safety net to avoid 'c:\' removal or spoiling by occasion
    if (folder.toPath().nameCount == 0)
        fail("Can't use root folder '$folder' as output")


    if (removeIfExists && folder.exists() && ! retry { folder.deleteRecursively() }
        && /* if delete failed (held by external process) but directory cleared it's ok */ !folder.list().isNullOrEmpty())
    {
        fail("Can't clear '$folder'")
    }


    if (folder.exists()) {
        if (!folder.isDirectory) fail("Not a folder: '$folder'")
    }
    else if (! retry { folder.mkdirs() })
        fail("Can't create folder '$folder'")
}


private fun collectClasses(classLoader: ClassLoader,
                           namespacePrefixes: Array<String>,
                           verbose: Boolean) : List<Class<*>> {

    val classes = classLoader.scanForClasses(*namespacePrefixes).toMutableList()
    classes.sortBy { it.name }
    if (verbose) println("${classes.count()} classes found")
    return classes
}


private fun collectTopLevels(
    classes: List<Class<*>>,
    verbose: Boolean
): List<Toplevel> {

    val toplevelClasses = classes.filter { Toplevel::class.java.isAssignableFrom(it) }
        .filter { !(it.isInterface || Modifier.isAbstract(it.modifiers)) }.toList()
    if (verbose) println("${toplevelClasses.size} toplevel class${(toplevelClasses.size != 1).condstr { "es" }} found")


    val toplevels = toplevelClasses.map {
        val kclass = it.kotlin
        if (kclass.constructors.any())
            it.getDeclaredConstructor().newInstance()
        else
            kclass.objectInstance
    }.filterIsInstance(Toplevel::class.java).sortedWith(compareBy({ it.root.name }, { it.toString() }))

    if (verbose) {
        println()
        println("Toplevels to generate:")
        toplevels.forEach { println("  $it") }
    }
    return toplevels
}


private fun collectSortedGeneratorsToInvoke(
    roots: List<Root>,
    classes: List<Class<*>>,
    filterByGeneratorClassSimpleName: Regex,
    verbose: Boolean,
    gradleGenerationSpecs: List<GradleGenerationSpec>
): List<IGeneratorAndRoot> {

    val hardcoded = roots.flatMap { root -> root.hardcodedGenerators.map { ExternalGenerator(it, root) } }

    val gradle = gradleGenerationSpecs.map { it.toGeneratorAndRoot(roots) }

    val external = classes.filter { IGeneratorAndRoot::class.java.isAssignableFrom(it) }.mapNotNull { it.kotlin.objectInstance }.filterIsInstance<ExternalGenerator>()

    if (verbose) {
        println()
        println("Collecting generators (filtering generator's `javaClass.simpleName` by regex '$filterByGeneratorClassSimpleName'):")
        println("  ${hardcoded.size}" + " hardcoded generator(s) -- specified directly in 'Root' constructor")
        println("  ${gradle.size}"  + " gradle generator(s)    -- specified in gradle's rdgen plugin 'rdgen { generator { ...} }' sections")
        println("  ${external.size}"  + " external generator(s)  -- specified by extending kotlin object from class GenerationUnit")
        println()
    }

    return hardcoded
        .plus(gradle)
        .plus(external)
        .filter { (gen, root) ->
            val shouldGenerate = filterByGeneratorClassSimpleName.containsMatchIn(gen.javaClass.simpleName) && !gen.folder.toString().contains(InvalidSysproperty)

            if (verbose)
                println((if (shouldGenerate) "++  MATCHED TO  ++" else "-- FILTERED OUT --") + " '$root' + $gen ")

            shouldGenerate
        }.sorted()
}
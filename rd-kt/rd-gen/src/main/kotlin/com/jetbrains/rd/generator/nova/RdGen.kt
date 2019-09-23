package com.jetbrains.rd.generator.nova

import com.jetbrains.rd.generator.gradle.GradleGenerationSpec
import com.jetbrains.rd.generator.nova.util.InvalidSysproperty
import com.jetbrains.rd.util.getThrowableText
import com.jetbrains.rd.util.hash.PersistentHash
import com.jetbrains.rd.util.kli.Kli
import com.jetbrains.rd.util.reflection.scanForClasses
import com.jetbrains.rd.util.reflection.scanForResourcesContaining
import com.jetbrains.rd.util.reflection.usingValue
import com.jetbrains.rd.util.string.condstr
import java.io.File
import java.io.PrintStream
import java.lang.Class
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.measureTimeMillis

class RdGen : Kli() {

    companion object {
        /**
         * Moving this field forward you trigger rebuild even if inputs and output of generator hasn't changed.
         */
        const val version = "1.07"

        /**
         * File to store all information for incremental work
         */
        const val hashFileName = ".rdgen"
    }


    override val description: String
        get() = "RD Generator, v$version. Search for inheritors of '${Toplevel::class.java.name}'" +
            " and generate sources according generators: inheritors of '${IGenerator::class.java.name}'."

    override val comments: String
        get() = "Generates RD Model from DSL "


    var compilerClassloader : ClassLoader? = null

    val sources =       option_string('s',    "source", "Folders with dsl .kt files. If not present, scan classpath for inheritors of '${Toplevel::class.java.name}'")
    val hashFolder =    option_path(  'h',    "hash-folder","Folder to store hash file '$hashFileName' for incremental generation", Paths.get("").toAbsolutePath())
    val compiled =      option_path(  null,   "compiled", "Folder for compiled dsl. Temporary folder is created if option is not specified.")
    val classpath =     option_string('c',    "compiler-classpath","Classpath for kotlin compiler. You must specify it if you referenced something from your dsl" )


    val force =         option_flag(  'f',   "force", "Suppress incremental generation.")
    val clearOutput =   option_flag(  'x',   "clear", "Clear output folder before generation (if it is not incremental) ")

    val packages =      option_string('p',    "packages", "Java package names to search toplevels, delimited by ','. Example: com.jetbrains.rd.model.nova", "com,org")
    val filter =        option_string(null,   "filter", "Filter generators by searching regular expression inside generator class simple name (case insensitive). Example: kotlin|csharp|cpp")
    val verbose =       option_flag(  'v',    "verbose", "Verbose output")

    val gradleGenerationSpecs = mutableListOf<GradleGenerationSpec>()

    val hashfile : Path get() = Paths.get(hashFolder.value!!.toString(), hashFileName).normalize()

    private fun v(msg: String) { if (verbose.value) println(msg) }
    private fun v(e: Throwable) { if (verbose.value) e.printStackTrace() }

//    val generationSpecs = mutableListOf<GradleGenerationSpec>()
//    val sourcePaths = mutableListOf<File>()

    private fun compile0(src: List<File>, dst: Path) : String? {

        v("Searching for Kotlin compiler")
        try {
            val compilerClass = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
            val clazz = compilerClassloader?.let {
                v("Using special classloader for kotlin compiler: $it")
                Class.forName(compilerClass, true, it)
            }?: Class.forName(compilerClass)


            val method = clazz.getMethod("exec", PrintStream::class.java, arrayOf("").javaClass)
            v("Compiling sources from '${src.joinToString { it.absolutePath } }' to '${dst.toAbsolutePath()}'")


            val userCp = classpath.value?:""
            v("User classpath: '$userCp'")


            val defaultCp = (
                    defaultClassloader.scanForResourcesContaining(javaClass.`package`.name) +
                    defaultClassloader.scanForResourcesContaining("kotlin")
            ).toSet().joinToString(System.getProperty("path.separator"))
            v("Rdgen default classpath: '$defaultCp'")

            val cp = listOf(userCp, defaultCp).filter { !it.isBlank() }.joinToString ( System.getProperty("path.separator") )
            v("Resulting kotlinc classpath: '$cp'")

            val args = listOf(
                "-cp", cp,
                "-d", dst.toString(),
                "-jvm-target", "1.8"
            ) + src.map { it.absolutePath }

            v("kotlinc " + args.joinToString(" ") { if (it.contains(' ')) "\"$it\"" else it })
            val returnCode = method.invoke(clazz.getDeclaredConstructor().newInstance(), System.err, args.toTypedArray()) as kotlin.Enum<*>


            return if (returnCode.ordinal == 0) {
                null //success
            } else {
                "Compilation failed. Return code: ${returnCode.ordinal}($returnCode)."
            }

        } catch (e: Throwable) {
            v(e)

            return if (e is ClassNotFoundException) {
                "Can't find K2JVMCompiler, maybe you forgot to add 'kotlin-compiler.jar' into classpath?"

            } else if (e is NoSuchMethodException || e is IllegalStateException || e is NullPointerException || e is InstantiationError || e is ExceptionInInitializerError) {
                "Found compiler, but can't run it properly. Exception: ${e.getThrowableText()}." + System.lineSeparator() + "Maybe, compiler version is not correct?"

            } else if (e is InvocationTargetException) {
                "Compilation failed with exception: " + e.targetException.getThrowableText()

            } else {
                "Unknown exception:  " + e.getThrowableText()
            }
        }
    }

    private val defaultClassloader = RdGen::class.java.classLoader!!

    fun compileDsl(src: List<File>) : ClassLoader? {
        val dst = compiled.value?.apply {
            v("Compiling into specified folder: $this")

        } ?: Files.createTempDirectory("rdgen-").apply {
            v("Temporary folder created: $this")
            toFile().deleteOnExit()
        }


        val elapsed = measureTimeMillis {
            error = compile0(src, dst)
        }
        v("Compilation finished in $elapsed ms")


        return if (error == null)
            try {
                val classpathUris = listOf(dst.toUri().toURL()) + (classpath.value?.let {
                    it.split(File.pathSeparatorChar).map { File(it).toURI().toURL() }
                } ?: emptyList())

                v("Loading compiled classes from '${classpathUris.joinToString()}'")
                URLClassLoader(classpathUris.toTypedArray(), defaultClassloader)
            } catch (e: Throwable) {
                error = "Error during loading classes from '$dst'"
                null
            }
        else null
    }


    private fun errorAndExit(msg: String? = null) : Boolean {
        System.err.apply {
            println(msg?:error)
            println()
            println(help())
        }
        return false
    }


    private val outputFolderKey = "outputFolders"

    /**
     * Creates special (serializable in file) hash for incremental generation
      */
    private fun prepareHash(outputFolders: Collection<File>): PersistentHash {
        return PersistentHash().also { res ->

            //sources
            sources.value?. let {
                for (src in it.split(';')) {
                    res.mixFileRecursively(File(src))
                }
            }
//            for (sourcePath in sourcePaths) {
//                res.mixFileRecursively(sourcePath)
//            }

            //output
            outputFolders.forEach { res.mixFileRecursively(it) { file -> file.isFile && file.name != hashFileName } } //if you store hashFile in output
            outputFolders.forEach { folder -> res.mix(outputFolderKey, folder.canonicalPath) }

            //generator contents
            defaultClassloader.scanForResourcesContaining(javaClass.`package`.name).forEach { file ->
                res.mixFileRecursively(file) {f -> f.isFile && (f.name.endsWith(".jar") || f.name.endsWith(".class"))}
            }
            //no need for version if generator contents changes are tracked
            res.mix("version", version)


            res.mix("userClasspath", classpath.value)
            res.mix("filter", filter.value)
            res.mix("packages", packages.value)

        }
    }


    /**
     * Should we regenerate everything or not? Only applicablr for generation based on source code .kt dsl.
     * Generation upon classpath is always non-incremental.
     *
     */
    private fun changedSinceLastStart(): Boolean {
        if (sources.value == null) return true //no incremental generation from classpath

        v("Reading hash from $hashfile")
        val oldHash = PersistentHash.load(hashfile)

        val outputFolders = oldHash[outputFolderKey].map { File(it) }
        val newHash = prepareHash(outputFolders)

        oldHash.firstDiff(newHash)?.let {
            v("Hashes are different at key '$it', oldHash: ${oldHash[it].joinToString(",")}, newHash:${newHash[it].joinToString(",")}")
            return true
        }

        //This is very special hack for msbuild. We need to modify this file always when update packages.config file.
        if (hashfile.toFile().exists()) hashfile.toFile().setLastModified(System.currentTimeMillis())

        return false
    }


    /**
     * Main method.
     * 1. Check something is not up-to-date (if [RdGen.force] is 'true' then always regenerate)
     * 2. Compile rd model DSL if needed (or use current classloader to find compiled rd model)
     * 3. Invoke [generateRdModel]
     */
    fun run(): Boolean {
        if (error != null) return errorAndExit()

        //0. Parsing generation parameters
        val generatorFilter = try {
            filter.value?.let { Regex(it, RegexOption.IGNORE_CASE) }
        } catch (e: Throwable) {
            return errorAndExit("Can't parse regex '${filter.value}'")
        }

        val pkgPrefixes = packages.value!!.split(',', ' ', ':', ';').toTypedArray()


        //1. Check whether do we need to generate anything?
        if (!force.value) {
            if (!changedSinceLastStart()) {
                v("No changes since last start")
                return true
            }
        } else {
            v("Forced: true")
            compiled.value?.let {
                v("Clearing '$it'")
                it.toFile().deleteRecursively()
            }
        }

        //2. Compile dsl or take defaultClassloader
        val srcDir = sources.value
        val classloader =
            if (srcDir != null) {
                val sourcePaths = (srcDir.split(';').map { File(it) })
                for (sourcePath in sourcePaths) {
                    if (!sourcePath.isDirectory)
                        return errorAndExit("Sources are incorrect. No folder found at '$sourcePath'")
                }

                compileDsl(sourcePaths) ?: return errorAndExit()
            } else {
                println("Folder 'source' isn't specified, searching for models in classloader of RdGen class (current java process classpath).")
                println("To see parameters and usages invoke `rdgen -h`")
                defaultClassloader
            }


        //3. Find all rd model classes in classpath and generate code
        val outputFolders = try {
            generateRdModel(classloader, pkgPrefixes, verbose.value, generatorFilter, clearOutput.value, gradleGenerationSpecs)
        } catch (e : Throwable) {
            e.printStackTrace()
            return false
        }

        //4. Serialize .rdgen hashfile for incremental generation in future
        if (srcDir != null) {
            val hash = prepareHash(outputFolders)
            v("Storing hash into '$hashfile'")
            hash.store(hashfile)
        } else {
            v("Don't store hashfile, build is not incremental since 'sources' aren't specified")
        }

        return true
    }
}

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
                .map { it.canonicalPaths }.flatten()
                .none { path -> gen.canonicalPaths.any { it == path || it.startsWith(path + File.separator)} }

        //Here is the real part
        if (verbose) println("Invoke $gen on $root, clearFolder=$shouldClear")

        ::settingCtx.usingValue(gen) {
            gen.generate(root, shouldClear, toplevels.filter { it.root == root })
        }

        generatedFolders.addAll(gen.folders)
    }

    val endTime = System.currentTimeMillis()
    if (verbose) println("Generation finished in ${endTime - startTime} ms")

    return generatedFolders
}


private fun collectClasses(classLoader: ClassLoader,
                           namespacePrefixes: Array<String>,
                           verbose: Boolean) : List<Class<*>> {

    val classes = classLoader.scanForClasses(*namespacePrefixes).toList()
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
): List<IGenerationUnit> {

    val hardcoded = roots.flatMap { root -> root.hardcodedGenerators.map { GenerationUnit(it, root) } }

    val fromSpec = gradleGenerationSpecs.map { it.toGenerationUnit(roots) }

    val external = classes.filter { IGenerationUnit::class.java.isAssignableFrom(it) }.mapNotNull { it.kotlin.objectInstance }.filterIsInstance<GenerationUnit>()

    if (verbose) {
        println()
        println("Collecting generators (filtering by regex '$filterByGeneratorClassSimpleName'):")
        println("  ${hardcoded.size}" + " hardcoded generator(s) -- specified directly in 'Root' constructor")
        println("  ${fromSpec.size}"  + " gradle generator(s)    -- specified in gradle's rdgen plugin 'rdgen { generator { ...} }' sections")
        println("  ${external.size}"  + " external generator(s)  -- specified by extending kotlin object from class GenerationUnit")
        println()
    }

    return hardcoded
            .plus(fromSpec)
            .plus(external)
            .filter { (gen, root) ->
        val shouldGenerate = filterByGeneratorClassSimpleName.containsMatchIn(gen.javaClass.simpleName) && gen.folders.none { it.toString().contains(InvalidSysproperty) }

        if (verbose)
            println((if (shouldGenerate) "++  MATCHED TO  ++" else "-- FILTERED OUT --") + " '$root' + $gen ")

        shouldGenerate
    }.sorted()
}

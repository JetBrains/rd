package com.jetbrains.rider.generator.nova

import com.jetbrains.rider.util.getThrowableText
import com.jetbrains.rider.util.hash.PersistentHash
import com.jetbrains.rider.util.kli.Kli
import com.jetbrains.rider.util.reflection.scanForClasses
import com.jetbrains.rider.util.reflection.scanForResourcesContaining
import com.jetbrains.rider.util.string.condstr
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
        const val version = "1.01"
        const val hashFileName = ".rdgen"
    }


    override val description: String
        get() = "RD Generator, v$version. Search for inheritors of '${Toplevel::class.java.name}'" +
            " and generate sources according generators: inheritors of '${IGenerator::class.java.name}'."

    override val comments: String
        get() = "Generates RD Model from DSL "


    var compilerClassloader : ClassLoader? = null

    val sources =       option_path(  's',    "source", "Folder with dsl .kt files. If not present, scan classpath for inheritors of '${Toplevel::class.java.name}'")
    val hashFolder =    option_path(  'h',    "hash-folder","Folder to store hash file '$hashFileName' for incremental generation", Paths.get("").toAbsolutePath())
    val compiled =      option_path(  null,   "compiled", "Folder for compiled dsl. Temporary folder is created if option is not specified.")
    val classpath =     option_string('c',    "compiler-classpath","Classpath for kotlin compiler. You must specify it if you referenced something from your dsl" )


    val force =         option_flag(  'f',   "force", "Suppress incremental generation.")
    val clearOutput =   option_flag(  'x',   "clear", "Clear output folder before generation (if it is not incremental) ")

    val packages =      option_string('p',    "packages", "Java package names to search toplevels, delimited by ','. Example: com.jetbrains.rider.model.nova", "com,org")
    val filter =        option_string(null,   "filter", "Filter generators by searching regular expression inside generator class simple name (case insensitive). Example: kotlin|csharp")
    val verbose =       option_flag(  'v',    "verbose", "Verbose output")

    val hashfile : Path get() = Paths.get(hashFolder.value!!.toString(), hashFileName)

    private fun v(msg: String) { if (verbose.value) println(msg) }
    private fun v(e: Throwable) { if (verbose.value) e.printStackTrace() }


    private fun compile0(src: Path, dst: Path) : String? {

        v("Searching for Kotlin compiler")
        try {
            val compilerClass = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
            val clazz = compilerClassloader?.let {
                v("Using special classloader for kotlin compiler: $it")
                Class.forName(compilerClass, true, it)
            }?: Class.forName(compilerClass)


            val method = clazz.getMethod("exec", PrintStream::class.java, arrayOf("").javaClass)
            v("Compiling sources from '${src.toAbsolutePath()}' to '${dst.toAbsolutePath()}'")


            val userCp = classpath.value?:""
            v("User classpath: '$userCp'")


            val defaultCp = (
                    defaultClassloader.scanForResourcesContaining(javaClass.`package`.name) +
                    defaultClassloader.scanForResourcesContaining("kotlin")
            ).toSet().joinToString(System.getProperty("path.separator"))
            v("Rdgen default classpath: '$defaultCp'")

            val cp = listOf(userCp, defaultCp).filter { !it.isBlank() }.joinToString ( System.getProperty("path.separator") )
            v("Resulting kotlinc classpath: '$cp'")

            val args = arrayOf(
                "-cp", cp,
                "-d", dst.toString(),
                src.toString()
            )
            v("kotlinc "+args.joinToString(" ") { if (it.contains(' ')) "\"$it\"" else it })
            val returnCode = method.invoke(clazz.newInstance(), System.err, args) as kotlin.Enum<*>


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

    private fun compileDsl(src: Path) : ClassLoader? {
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
                v("Loading compiled classes from '$dst'")
                URLClassLoader(arrayOf(dst.toUri().toURL()), defaultClassloader)
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
    private fun prepareHash(outputFolders: Collection<File>): PersistentHash {
        return PersistentHash().also { res ->

            //sources
            sources.value?. let { res.mixFileRecursively(it.toFile()) }

            //output
            outputFolders.forEach { res.mixFileRecursively(it) { file -> file.isFile && file.name != hashFileName } } //if you store hashFile in output
            outputFolders.forEach { folder -> res.mix(outputFolderKey, folder.toString()) }

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


    private fun changedSinceLastStart():Boolean {
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


    fun run() : Boolean {
        if (error != null) return errorAndExit()

        //parsing parameters
        val generatorFilter = try {
            filter.value?.let { Regex(it, RegexOption.IGNORE_CASE) }
        } catch (e: Throwable) {
            return errorAndExit("Can't parse regex '${filter.value}'")
        }

        val pkgPrefixes = packages.value!!.split(',', ' ', ':', ';').toTypedArray()


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

        val srcDir = sources.value
        val classloader =
            if (srcDir != null) {
                if (!srcDir.toFile().isDirectory) return errorAndExit("Sources are incorrect. No folder found at '$srcDir'")
                compileDsl(srcDir) ?: return errorAndExit()
            } else {
                println("Folder 'source' isn't specified, searching for models in classloader of RdGen class (current java process classpath)")
                defaultClassloader
            }


        val outputFolders = try {
            generateRdModel(classloader, pkgPrefixes, verbose.value, generatorFilter, clearOutput.value)
        } catch (e : Throwable) {
            e.printStackTrace()
            return false
        }

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


fun generateRdModel(classLoader: ClassLoader, namespacePrefixes: Array<String>
                    , verbose: Boolean = false
                    , generatorsFilter : Regex? = null
                    , clearOutputFolderIfExists: Boolean = false
) : Set<File> {
    val startTime = System.currentTimeMillis()

    val genfilter = generatorsFilter ?: Regex(".*")

    if (verbose) {
        println()
        println("RdGen model generator started")
        println("Searching classes with namespace prefixes: '${namespacePrefixes.joinToString(", ")}'")
        println("Generator's filter: '${genfilter.pattern}'")
    }

    val classes = classLoader.scanForClasses(*namespacePrefixes).toList()
    if (verbose) println("${classes.count()} classes found")

    val toplevelClasses = classes.filter { Toplevel::class.java.isAssignableFrom(it)}.filter { ! (it.isInterface || Modifier.isAbstract(it.modifiers))  }.toList()
    if (verbose) println("${toplevelClasses.size} toplevel class${(toplevelClasses.size != 1).condstr {"es"}} found")

    val toplevels = toplevelClasses.map {
        val kclass = it.kotlin
        if (kclass.constructors.any())
            it.newInstance()
        else
            kclass.objectInstance
    }.filterIsInstance(Toplevel::class.java).sortedWith (compareBy({it.root.name}, {it.toString()}))

    if (verbose) {
        println("Toplevels to generate:")
        toplevels.forEach(::println)
    }


    //Need to sort generators because we plan to purge generation folders sometimes
    data class GenPair(val generator: IGenerator, val root: Root) : Comparable<GenPair> {
        override fun compareTo(other: GenPair): Int {
            generator.folder.canonicalPath.compareTo(other.generator.folder.canonicalPath).let { if (it != 0) return it}
            root.name.compareTo(other.root.name).let { if (it != 0) return it }
            generator.javaClass.name.compareTo(other.generator.javaClass.name).let { if (it != 0) return it }

            return 0 //sort is stable so, don't worry much
        }
    }

    val generatorsToInvoke = ArrayList<GenPair>()
    val validationErrors = ArrayList<String>()

    toplevels.map {it.root}.distinct().forEach { root ->
        if (verbose) println("Scanning $root, ${root.generators.size} generators found")

        root.generators.forEach { gen ->
            val shouldGenerate = genfilter.containsMatchIn(gen.javaClass.simpleName) && gen.folder.toString().isNotEmpty()
            if (verbose) println("  $gen: "+ if (shouldGenerate) "matches filter" else "--FILTERED OUT--")
            if (shouldGenerate) generatorsToInvoke.add(GenPair(gen, root))
        }

        root.initialize()
        root.validate(validationErrors)
    }

    if (validationErrors.isNotEmpty())
        throw GeneratorException("Model validation fail:" +
            validationErrors.joinToString ("") {"\n\n>> $it"}
        )

    generatorsToInvoke.sort()
    if (verbose) {
        println()
        println(">> Generating: ${generatorsToInvoke.size} filtered generators")
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
        if (verbose) println("Invoke $gen on $root, clearFolder=$shouldClear")
        gen.generate(root, shouldClear, toplevels.filter { it.root == root })

        generatedFolders.add(gen.folder)
    }

    val endTime = System.currentTimeMillis()
    if (verbose) println("Generation finished in ${endTime - startTime} ms")

    return generatedFolders
}



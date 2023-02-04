package com.jetbrains.rd.generator.nova

import com.jetbrains.rd.generator.nova.util.usingSystemProperty
import com.jetbrains.rd.util.getThrowableText
import com.jetbrains.rd.util.hash.PersistentHash
import com.jetbrains.rd.util.kli.Kli
import com.jetbrains.rd.util.reflection.scanForResourcesContaining
import java.io.File
import java.io.PrintStream
import java.lang.Class
import java.lang.reflect.InvocationTargetException
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
        const val version = "1.11"

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

    val noLineNumbersInComments = option_flag('n', "no-line-numbers", "Don't save original source line numbers in comments inside of generated files")

    val generatorsFile = option_string('g', "generators", "Path to the file with serialized GeneratorSpecs")

    val gradleGenerationSpecs: List<GenerationSpec>
        get() {
            val path = generatorsFile.value
            return if (path == null) emptyList()
            else GenerationSpec.loadFrom(File(path))
        }

    val hashfile : Path get() = Paths.get(hashFolder.value!!.toString(), hashFileName).normalize()

    private fun v(msg: String) { if (verbose.value) println(msg) }
    private fun v(e: Throwable) { if (verbose.value) e.printStackTrace() }

//    val generationSpecs = mutableListOf<GradleGenerationSpec>()
//    val sourcePaths = mutableListOf<File>()

    private fun compile0(src: List<File>, dst: Path) : String? {
        if (src.isEmpty()) {
            throw Exception("Input file list is empty, compilation aborted")
        }

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
                "-no-stdlib",
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
            res.mix("noLineNumbersInComments", noLineNumbersInComments.value.toString())
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
                val sourcePaths = (srcDir.split(';').filter { it.isNotEmpty() }.map { File(it) })
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
        v("gradleGenerationSpecs=[${gradleGenerationSpecs.joinToString("\n")}]")
        v("noLineNumbersInComments=${noLineNumbersInComments.value}")
        //3. Find all rd model classes in classpath and generate code
        val outputFolders = try {
            usingSystemProperty(
                SharedGeneratorSettings.LineNumbersInCommentsEnv,
                (!noLineNumbersInComments.value).toString()
            ) {
                generateRdModel(
                    classloader,
                    pkgPrefixes,
                    verbose.value,
                    generatorFilter,
                    clearOutput.value,
                    gradleGenerationSpecs
                )
            }
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



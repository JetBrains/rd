package com.jetbrains.rd.generator.nova

import com.jetbrains.rd.generator.nova.util.usingSystemProperty
import com.jetbrains.rd.util.kli.Kli
import java.io.Closeable
import java.io.File
import java.nio.file.Path

class RdGen : Kli() {

    companion object {
        /**
         * Moving this field forward you trigger rebuild even if inputs and output of generator hasn't changed.
         */
        const val version = "1.13"
    }


    override val description: String
        get() = "RD Generator, v$version. Search for inheritors of '${Toplevel::class.java.name}'" +
            " and generate sources according generators: inheritors of '${IGenerator::class.java.name}'."

    override val comments: String
        get() = "Generates RD Model from DSL "


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

    private fun v(msg: String) { if (verbose.value) println(msg) }

    private val defaultClassloader = RdGen::class.java.classLoader!!

    data class ClassLoaderResource(
        val classLoader: ClassLoader?,
        val tempDirectory: Path?,
        val ownClassLoader: Boolean = true
    ) : Closeable {

        override fun close() {
            if (ownClassLoader && classLoader is Closeable) {
                classLoader.close()
            }
            tempDirectory?.toFile()?.deleteRecursively()
        }
    }

    private fun errorAndExit(msg: String? = null) : Boolean {
        System.err.apply {
            println(msg?:error)
            println()
            println(help())
        }
        return false
    }


    /**
     * Main method.
     * 1. Find model classes in the current class loader.
     * 2. Invoke [generateRdModel].
     */
    fun run(): Boolean {
        if (error != null) return errorAndExit()

        //0. Parsing generation parameters
        val generatorFilter = try {
            filter.value?.let { Regex(it, RegexOption.IGNORE_CASE) }
        } catch (e: Throwable) {
            return errorAndExit("Can't parse regex '${filter.value}: ${e.message ?: "no message"}'")
        }

        val pkgPrefixes = packages.value!!.split(',', ' ', ':', ';').toTypedArray()

        //1. Prepare the class loader.
        println("Searching for models in classloader of RdGen class (current java process classpath).")
        println("To see parameters and usages invoke `rdgen -h`")
        val resource = ClassLoaderResource(defaultClassloader, null, ownClassLoader = false)

        resource.use { (classLoader) ->
            if (classLoader == null) {
                return errorAndExit()
            }

            v("gradleGenerationSpecs=[${gradleGenerationSpecs.joinToString("\n")}]")
            v("noLineNumbersInComments=${noLineNumbersInComments.value}")
            //2. Find all rd model classes in classpath and generate code
            try {
                usingSystemProperty(
                    SharedGeneratorSettings.LineNumbersInCommentsEnv,
                    (!noLineNumbersInComments.value).toString()
                ) {
                    generateRdModel(
                        classLoader,
                        pkgPrefixes,
                        verbose.value,
                        generatorFilter,
                        clearOutput.value,
                        gradleGenerationSpecs
                    )
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                return false
            }

            return true
        }
    }
}



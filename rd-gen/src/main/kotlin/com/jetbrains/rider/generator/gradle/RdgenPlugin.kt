package com.jetbrains.rider.generator.gradle

import com.jetbrains.rider.generator.nova.GenerationSpec
import com.jetbrains.rider.generator.nova.RdGen
import com.jetbrains.rider.util.Statics
import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.*

class RdgenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("rdgen", RdgenParams::class.java, project)
        project.tasks.create("rdgen", RdgenTask::class.java)
    }
}

open class RdgenParams @JvmOverloads constructor(val project: Project, val task: Task? = null) {
    constructor(task: Task) : this(task.project, task) {
    }

    private val projectExtension get() = project.extensions.getByType(RdgenParams::class.java)

//    val message: Property<String> = project.objects.property(String::class.java)

//  option_path('s', "source", "Folder with dsl .kt files. If not present, scan classpath for inheritors of '${Toplevel::class.java.name}'")
    private val sources = mutableListOf<Any>()

//            option_path(  'h',    "hash-folder","Folder to store hash file '${RdGen.hashFileName}' for incremental generation", Paths.get("").toAbsolutePath())
    val hashFolder = project.objects.property(String::class.java)

    //    option_path(  null,   "compiled", "Folder for compiled dsl. Temporary folder is created if option is not specified.")
    val compiled = project.objects.property(String::class.java)

//            option_string('c',    "compiler-classpath","Classpath for kotlin compiler. You must specify it if you referenced something from your dsl" )
    private val classpath = mutableListOf<Any>()

//            option_flag(  'f',   "force", "Suppress incremental generation.")
    val force = project.objects.property(Boolean::class.javaObjectType)

//            option_flag(  'x',   "clear", "Clear output folder before generation (if it is not incremental) ")
    val clearOutput = project.objects.property(Boolean::class.javaObjectType)

//            option_string('p',    "packages", "Java package names to search toplevels, delimited by ','. Example: com.jetbrains.rider.model.nova", "com,org")
    val packages =  project.objects.property(String::class.java)

//            option_string(null,   "filter", "Filter generators by searching regular expression inside generator class simple name (case insensitive). Example: kotlin|csharp")
    val filter = project.objects.property(String::class.java)

//            option_flag(  'v',    "verbose", "Verbose output")
    val verbose = project.objects.property(Boolean::class.javaObjectType)

    //for passing system properties
    val properties : Properties = Properties()

    private val generators =  mutableListOf<GenerationSpec>()
    fun generator(closure: Closure<GenerationSpec>): GenerationSpec {
        val generationSpec = project.configure(GenerationSpec(), closure) as GenerationSpec
        generators.add(generationSpec)
        return generationSpec
    }

    fun generator(closure: GenerationSpec.() -> Unit): GenerationSpec {
        return GenerationSpec().apply {
            closure()
            generators.add(this)
        }
    }

    fun sources(vararg paths: Any) {
        sources.addAll(paths)
    }

    fun classpath(vararg paths: Any) {
        classpath.addAll(paths)
    }

    private fun List<Any>.evalCallbacks() = map { if (it is Function0<*>) it.invoke() else it }

    fun evalProperties() = if (properties.isNotEmpty()) properties else projectExtension.properties

    fun evalSources() =
        if (sources.isNotEmpty())
            project.files(sources.evalCallbacks())
        else
            project.files(projectExtension.sources.evalCallbacks())

    fun evalHashFolder() = hashFolder.orNull ?: projectExtension.hashFolder.orNull
    fun evalCompiled() = compiled.orNull ?: projectExtension.compiled.orNull
    fun evalClasspath() =
        if (classpath.isNotEmpty())
            project.files(classpath.evalCallbacks())
        else
            project.files(projectExtension.classpath.evalCallbacks())
    fun evalPackages() = packages.orNull ?: projectExtension.packages.orNull
    fun evalFilter() = filter.orNull ?: projectExtension.filter.orNull
    fun evalGenerators() = if (generators.isNotEmpty()) generators else projectExtension.generators

    fun evalForce(): Boolean = force.orNull ?: (projectExtension.force.orNull ?: false)
    fun evalVerbose(): Boolean = verbose.orNull ?: (projectExtension.verbose.orNull ?: false)
    fun evalClearOutput(): Boolean = clearOutput.orNull ?: (projectExtension.clearOutput.orNull ?: false)
}

open class RdgenTask : DefaultTask() {
    private val params: RdgenParams = extensions.create("params", RdgenParams::class.java, this)

    @TaskAction
    fun run() {
//        params.properties.putAll(System.getProperties())


        Statics<Properties>().use(params.evalProperties()) {
            val rdGen = RdGen().apply {
                sourcePaths.addAll(params.evalSources().files)
                hashFolder.parse(params.evalHashFolder())
                compiled.parse(params.evalCompiled())
                classpath.parse(params.evalClasspath().files.joinToString(File.pathSeparator) { it.path })
                packages.parse(params.evalPackages())
                filter.parse(params.evalFilter())
                generationSpecs.addAll(params.evalGenerators())

                force *= params.evalForce()
                verbose *= params.evalVerbose()
                clearOutput *= params.evalClearOutput()
            }

//            print("Press any key to continue: ")
//            System.`in`.read()
            if (!rdGen.run())
                throw IllegalStateException("Rd Generation failed!")
        }
    }
}

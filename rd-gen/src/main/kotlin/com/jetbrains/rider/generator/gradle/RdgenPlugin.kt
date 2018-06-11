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
    val classpath = project.objects.property(String::class.java)


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

    fun sources(vararg paths: Any) {
        sources.addAll(paths)
    }

    fun getSources() = if (sources.isNotEmpty()) project.files(sources) else project.files(projectExtension.sources)
    fun getHashFolder() = hashFolder.orNull ?: projectExtension.hashFolder.orNull
    fun getCompiled() = compiled.orNull ?: projectExtension.compiled.orNull
    fun getClasspath() = classpath.orNull ?: projectExtension.classpath.orNull
    fun getPackages() = packages.orNull ?: projectExtension.packages.orNull
    fun getFilter() = filter.orNull ?: projectExtension.filter.orNull
    fun getGenerators() = if (generators.isNotEmpty()) generators else projectExtension.generators

    fun getForce(): Boolean = force.orNull ?: (projectExtension.force.orNull ?: false)
    fun getVerbose(): Boolean = verbose.orNull ?: (projectExtension.verbose.orNull ?: false)
    fun getClearOutput(): Boolean = clearOutput.orNull ?: (projectExtension.clearOutput.orNull ?: false)
}

open class RdgenTask : DefaultTask() {
    private val params: RdgenParams = extensions.create("params", RdgenParams::class.java, this)

    @TaskAction
    fun run() {
//        params.properties.putAll(System.getProperties())


        Statics<Properties>().use(params.properties) {
            val rdGen = RdGen().apply {
                sourcePaths.addAll(params.getSources().files)
                hashFolder.parse(params.getHashFolder())
                compiled.parse(params.getCompiled())
                classpath.parse(params.getClasspath())
                packages.parse(params.getPackages())
                filter.parse(params.getFilter())
                generationSpecs.addAll(params.getGenerators())

                force *= params.getForce()
                verbose *= params.getVerbose()
                clearOutput *= params.getClearOutput()
            }

//            print("Press any key to continue: ")
//            System.`in`.read()
            if (!rdGen.run())
                throw IllegalStateException("Rd Generation failed!")
        }
    }
}

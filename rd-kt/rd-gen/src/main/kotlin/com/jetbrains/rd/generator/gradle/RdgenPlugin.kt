package com.jetbrains.rd.generator.gradle

import com.jetbrains.rd.generator.nova.GenerationSpec
import com.jetbrains.rd.generator.nova.RdGen
import com.jetbrains.rd.util.Statics
import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.*
import kotlin.reflect.KProperty

class RdgenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        //global parameters for all rdgen-based tasks
        project.extensions.create("rdgen", RdgenParams::class.java, project)

        project.tasks.create("rdgen", RdgenTask::class.java)
    }
}

open class RdgenParams @JvmOverloads constructor(val project: Project, val task: Task? = null) {
    constructor(task: Task) : this(task.project, task)

//  option_path(  'h', "hash-folder","Folder to store hash file '${RdGen.hashFileName}' for incremental generation", Paths.get("").toAbsolutePath())
    var hashFolder : String? = null

//  option_path(  null,   "compiled", "Folder for compiled dsl. Temporary folder is created if option is not specified.")
    val compiled : String? = null

//  option_flag(  'f',   "force", "Suppress incremental generation.")
    var force: Boolean? = null

//  option_flag(  'x',   "clear", "Clear output folder before generation (if it is not incremental) ")
    var clearOutput: Boolean? = null

//  option_string('p',    "packages", "Java package names to search toplevels, delimited by ','. Example: com.jetbrains.rider.model.nova", "com,org")
    var packages : String? = null

//  option_string(null,   "filter", "Filter generators by searching regular expression inside generator class simple name (case insensitive). Example: kotlin|csharp|cpp")
    var filter : String? = null

//  option_flag(  'v',    "verbose", "Verbose output")
    var verbose: Boolean? = null


//  for passing system properties
    val properties : Properties = Properties()


    //specify multiple generators
    internal val generators =  mutableListOf<GenerationSpec>()
    fun generator(closure: Closure<GenerationSpec>) = GenerationSpec().apply {
        project.configure(this, closure)
        generators.add(this)
    }
    fun generator(closure: GenerationSpec.() -> Unit) = GenerationSpec().apply {
        closure()
        generators.add(this)
    }

    //specify multiple source folders
    internal val _sources = mutableListOf<Any>()
    fun sources(vararg paths: Any) { _sources.addAll(paths) }

    //specify classpath
    internal val _classpath = mutableListOf<Any>()
    fun classpath(vararg paths: Any) { _classpath.addAll(paths) }

}

open class RdgenTask : DefaultTask() {
    private val local: RdgenParams = extensions.create("params", RdgenParams::class.java, this)
    private val global: RdgenParams get() =  project.extensions.getByType(RdgenParams::class.java)

    fun<T> get(p: KProperty<T>) : T {
        var res = p.call(local)
        if (res == null || (res is Collection<*> && res.isEmpty()) || (res is Map<*,*> && res.isEmpty()))
            res = p.call(global)

        return res
    }

    fun files(p: KProperty<List<*>>) : Set<File> {
        val list = get(p)
        val res = list.map { if (it is Function0<*>) it.invoke() else it  }
        return project.files(res).files
    }

    @TaskAction
    fun run() {
        Statics<Properties>().use(get(RdgenParams::properties)) {
            val rdGen = RdGen().apply {
                sourcePaths.addAll( files(  RdgenParams::_sources))
                hashFolder.parse(   get(    RdgenParams::hashFolder))
                compiled.parse(     get(    RdgenParams::compiled))
                classpath.parse(    files(  RdgenParams::_classpath).joinToString(File.pathSeparator) { it.path })
                packages.parse(     get(    RdgenParams::packages))
                filter.parse(       get(    RdgenParams::filter))


                force *=            get(RdgenParams::force) ?: false
                verbose *=          get(RdgenParams::verbose) ?: false
                clearOutput *=      get(RdgenParams::clearOutput) ?: false

                generationSpecs.addAll(get(RdgenParams::generators))
            }

//            print("Press any key to continue: ")
//            System.`in`.read()
            if (!rdGen.run())
                throw IllegalStateException("Rd Generation failed!")
        }
    }
}

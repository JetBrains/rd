package com.jetbrains.rd.generator.gradle

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


    //specify multiple generators
    internal val generators =  mutableListOf<GradleGenerationSpec>()
    fun generator(closure: Closure<GradleGenerationSpec>) = GradleGenerationSpec().apply {
        project.configure(this, closure)
        generators.add(this)
    }
    fun generator(closure: GradleGenerationSpec.() -> Unit) = GradleGenerationSpec().apply {
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
    private val global: RdgenParams? = project.extensions.findByType(RdgenParams::class.java)

    fun <T> get(getter: (RdgenParams) -> T): T {
        val res = getter(local)
        if (global == null) return res
        return when {
            res == null -> getter(global)
            res is Collection<*> && res.isEmpty() -> getter(global)
            res is Map<*, *> && res.isEmpty() -> getter(global)
            else -> res
        }
    }

    fun files(getter: (RdgenParams) -> List<*>) : Set<File> {
        val list = get(getter)
        val res = list.map { if (it is Function0<*>) it() else it  }
        return project.files(res).files
    }

    @TaskAction
    fun run() {
        val rdGen = RdGen().apply {
            sources *=  files{  it._sources }.joinToString(";")
            hashFolder.parse(   get{    it.hashFolder})
            compiled.parse(     get{    it.compiled})
            classpath.parse(    files{  it._classpath}.joinToString(File.pathSeparator) { it.path })
            packages.parse(     get{    it.packages})
            filter.parse(       get{    it.filter})


            force *=            get{it.force} ?: false
            verbose *=          get{it.verbose} ?: false
            clearOutput *=      get{it.clearOutput} ?: false

            gradleGenerationSpecs.addAll(get{it.generators})
        }

//            print("Press any key to continue: ")
//            System.`in`.read()
        check(rdGen.run()) { "Rd Generation failed!" }
    }
}

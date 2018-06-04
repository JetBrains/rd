package com.jetbrains.rider.generator.gradle

import com.jetbrains.rider.generator.nova.GenerationSpec
import com.jetbrains.rider.generator.nova.RdGen
import com.jetbrains.rider.util.Statics
import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.util.*

class RdgenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("rdgen", RdgenParams::class.java, project)
        project.tasks.create("rdgen", RdgenTask::class.java)
    }
}

open class RdgenParams(val project: Project) {
//    val message: Property<String> = project.objects.property(String::class.java)

//  option_path('s', "source", "Folder with dsl .kt files. If not present, scan classpath for inheritors of '${Toplevel::class.java.name}'")
    val sources = project.objects.property(String::class.java)

//            option_path(  'h',    "hash-folder","Folder to store hash file '${RdGen.hashFileName}' for incremental generation", Paths.get("").toAbsolutePath())
    val hashFolder = project.objects.property(String::class.java)

    //    option_path(  null,   "compiled", "Folder for compiled dsl. Temporary folder is created if option is not specified.")
    val compiled = project.objects.property(String::class.java)

//            option_string('c',    "compiler-classpath","Classpath for kotlin compiler. You must specify it if you referenced something from your dsl" )
    val classpath = project.objects.property(String::class.java)


//            option_flag(  'f',   "force", "Suppress incremental generation.")
    val force = project.objects.property(java.lang.Boolean::class.java)

//            option_flag(  'x',   "clear", "Clear output folder before generation (if it is not incremental) ")
    val clearOutput = project.objects.property(java.lang.Boolean::class.java)

//            option_string('p',    "packages", "Java package names to search toplevels, delimited by ','. Example: com.jetbrains.rider.model.nova", "com,org")
    val packages =  project.objects.property(String::class.java)

//            option_string(null,   "filter", "Filter generators by searching regular expression inside generator class simple name (case insensitive). Example: kotlin|csharp")
    val filter = project.objects.property(String::class.java)

//            option_flag(  'v',    "verbose", "Verbose output")
    val verbose = project.objects.property(java.lang.Boolean::class.java)

    //for passing system properties
    val properties : Properties = Properties()

    val generators =  mutableListOf<GenerationSpec>()
    fun generator(closure: Closure<GenerationSpec>): GenerationSpec {
        val generationSpec = project.configure(GenerationSpec(), closure) as GenerationSpec
        generators.add(generationSpec)
        return generationSpec
    }
}

open class RdgenTask : DefaultTask() {
    @TaskAction
    fun run() {
        val params = project.extensions.getByType(RdgenParams::class.java)
//        params.properties.putAll(System.getProperties())


        Statics<Properties>().use(params.properties) {
            val rdGen = RdGen().apply {
                sources.parse(params.sources.orNull)
                hashFolder.parse(params.hashFolder.orNull)
                compiled.parse(params.compiled.orNull)
                classpath.parse(params.classpath.orNull)
                packages.parse(params.packages.orNull)
                filter.parse(params.filter.orNull)
                generationSpecs.addAll(params.generators)

                force *= params.force.orNull as? Boolean ?: false
                verbose *= params.verbose.orNull as? Boolean ?: false
                clearOutput *= params.clearOutput.orNull as? Boolean ?: false
            }

//            print("Press any key to continue: ")
//            System.`in`.read()
            if (!rdGen.run())
                throw IllegalStateException("Rd Generation failed!")
        }
    }
}
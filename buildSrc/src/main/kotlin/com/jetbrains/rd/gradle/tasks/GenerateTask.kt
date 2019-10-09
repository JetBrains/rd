import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.extra
import java.io.File
import kotlin.let

open class GenerateTask : JavaExec() {
    @Input
    lateinit var sourcesRoot: File
    @Input
    lateinit var sourcesFolder: String

    init {
        group = "generate"
        main = "com.jetbrains.rd.generator.nova.MainKt"
        outputs.upToDateWhen { true }
    }

    override fun exec() {
        println("Starting GenerateTask sourcesRoot=$sourcesRoot, sourcesFolder=$sourcesFolder")

        super.exec()

        val cppOutputDirectory = systemProperties["model.out.src.cpp.dir"] as? String
        cppOutputDirectory?.let { output ->
            project.copy {
                val cppRoot : File by project.rootProject.extra.properties
                from("$cppRoot/PrecompiledHeader.cmake")
                into(output)
            }
        } ?: println("Property \"model.out.src.cpp.dir\" is missing")

        println("Finishing GenerateTask sourcesRoot=$sourcesRoot, sourcesFolder=$sourcesFolder")
    }

    fun lateInit() {
        args = listOf("--source=$sourcesRoot/$sourcesFolder", "--hash-folder=${project.rootProject.buildDir}/hash/$sourcesFolder")
    }

    override fun setSystemProperties(properties: MutableMap<String, *>) {
        super.setSystemProperties(properties)

        outputs.dirs(properties.values.toList())
    }
}
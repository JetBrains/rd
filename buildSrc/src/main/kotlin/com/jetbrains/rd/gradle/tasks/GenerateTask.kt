import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
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
        super.exec()

        (systemProperties["model.out.src.cpp.dir"] as? String)?.let { output ->
/*
            project.copy {
                from("${project("cpp").projectDir}/PrecompiledHeader.cmake")
                into(output)
                //todo
            }
*/
        }
    }

    fun lateInit() {
        args = listOf("--source=$sourcesRoot/$sourcesFolder", "--hash-folder=${project.rootProject.buildDir}/hash/$sourcesFolder")
    }
}
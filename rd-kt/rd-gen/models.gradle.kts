import com.jetbrains.rd.gradle.tasks.RdGenerateTask
import com.jetbrains.rd.gradle.tasks.util.cppDirectorySystemPropertyKey
import com.jetbrains.rd.gradle.tasks.util.csDirectorySystemPropertyKey
import com.jetbrains.rd.gradle.tasks.util.ktDirectorySystemPropertyKey

val repoRoot: File by rootProject.extra.properties
val cppRoot: File by rootProject.extra.properties
val ktRoot: File by rootProject.extra.properties
val csRoot: File by rootProject.extra.properties

val BUILD_DIR = parent!!.buildDir

tasks {
    val sourcesRoot = ktRoot.resolve("rd-gen/src/models/kotlin/com/jetbrains/rd/models")

    fun RdGenerateTask.collectSources() {
        fun addSources(properties: Map<String, String>, sourcesFolder: String) {
            addSourceDirectory(sourcesRoot.resolve(sourcesFolder))
            addOutputDirectories(properties.mapKeys { "${it.key}.$sourcesFolder" })
        }

        addSources(mapOf(
                cppDirectorySystemPropertyKey to "${cppRoot}/demo",
                ktDirectorySystemPropertyKey to "${BUILD_DIR}/models/demo",
                csDirectorySystemPropertyKey to "${csRoot}/Test.Cross/obj/DemoModel"
        ), "demo")

        addSources(mapOf(
                cppDirectorySystemPropertyKey to "$cppRoot/src/rd_framework_cpp/src/test/util/interning",
                ktDirectorySystemPropertyKey to "$BUILD_DIR/models/interning"
        ), "interning")

        addSources(mapOf(
                cppDirectorySystemPropertyKey to "$cppRoot/src/rd_framework_cpp/src/test/util/entities"
        ), "entities")

        addSources(mapOf(
                ktDirectorySystemPropertyKey to "$BUILD_DIR/models/sync"
        ), "sync")

        addSources(mapOf(
                ktDirectorySystemPropertyKey to "$BUILD_DIR/models/openEntity"
        ), "openEntity")

        addSources(mapOf(
                csDirectorySystemPropertyKey to "${csRoot}/Test.RdFramework/Reflection/data/Generated"
        ), "reflectionTest")
    }

    @Suppress("UNUSED_VARIABLE")
    val generateEverything by creating(RdGenerateTask::class) {
        classpath(project.the<SourceSetContainer>()["main"]!!.compileClasspath
                .minus(files(gradle.gradleHomeDir?.resolve("lib")?.listFiles()?.filter { it.name.contains("kotlin-stdlib") || it.name.contains("kotlin-reflect") } ?: listOf<File>()))
        )
        classpath(project.the<SourceSetContainer>()["main"]!!.output)

        collectSources()

        val sourceFiles = sourceDirectories.joinToString(separator = ";") { it.absolutePath }
        val hashFolder = project.rootProject.buildDir
                .resolve("hash")
                .resolve("models")
        outputs.dirs(hashFolder)
        args = listOf("--source=$sourceFiles;", "--packages=com,org,testModels", "--hash-folder=$hashFolder", "-v")
    }
}
import com.jetbrains.rd.gradle.tasks.util.cppDirectorySystemPropertyKey
import com.jetbrains.rd.gradle.tasks.util.csDirectorySystemPropertyKey
import com.jetbrains.rd.gradle.tasks.util.ktDirectorySystemPropertyKey
import com.jetbrains.rd.gradle.tasks.RdGenerateTask

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
                csDirectorySystemPropertyKey to "${csRoot}/CrossTest/Model"
        ), "demo")

        addSources(mapOf(
                cppDirectorySystemPropertyKey to "$cppRoot/src/rd_framework_cpp/src/test/util/interning",
                ktDirectorySystemPropertyKey to "$BUILD_DIR/models/interning"
        ), "interning")

        addSources(mapOf(
                ktDirectorySystemPropertyKey to "$BUILD_DIR/models/test"
        ), "test")

        addSources(mapOf(
                cppDirectorySystemPropertyKey to "$cppRoot/src/rd_framework_cpp/src/test/util/entities"
        ), "entities")

        addSources(mapOf(
                ktDirectorySystemPropertyKey to "$BUILD_DIR/models/sync"
        ), "sync")
    }

    @Suppress("UNUSED_VARIABLE")
    val generateEverything by creating(RdGenerateTask::class) {
        classpath = project.the<SourceSetContainer>()["main"]!!.runtimeClasspath

        collectSources()

        val sourceFiles = sourceDirectories.joinToString(separator = ";") { it.absolutePath }
        args = listOf("--source=$sourceFiles;", "--hash-folder=${project.rootProject.buildDir}/hash/models", "-v")
    }
}
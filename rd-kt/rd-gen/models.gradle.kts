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
    fun creatingGenerateTask(properties: Map<String, String>) = creating(RdGenerateTask::class) {
        classpath = project.the<SourceSetContainer>()["main"]!!.runtimeClasspath

        sourcesRoot = ktRoot.resolve("rd-gen/src/models/kotlin/com/jetbrains/rd/models")
        sourcesFolder = "demo"

        addSourcesDirectories(properties)

        args = listOf("--source=$sourcesRoot/$sourcesFolder", "--hash-folder=${project.rootProject.buildDir}/hash/$sourcesFolder", "-v")
    }

    val generateDemoModel by creatingGenerateTask(mapOf(
            cppDirectorySystemPropertyKey to "${cppRoot}/demo",
            ktDirectorySystemPropertyKey to "${BUILD_DIR}/models/demo",
            csDirectorySystemPropertyKey to "${csRoot}/CrossTest/Model"
    ))

    val generateInterningTestModel by creatingGenerateTask(mapOf(
            cppDirectorySystemPropertyKey to "$cppRoot/src/rd_framework_cpp/src/test/util/interning",
            ktDirectorySystemPropertyKey to "$BUILD_DIR/models/interning"
    ))

    val generateTestModel by creatingGenerateTask(mapOf(
            ktDirectorySystemPropertyKey to "$BUILD_DIR/models/test"
    ))

    val generateCppTestEntities by creatingGenerateTask(mapOf(
            cppDirectorySystemPropertyKey to "$cppRoot/src/rd_framework_cpp/src/test/util/entities"
    ))

    @Suppress("UNUSED_VARIABLE")
    val generateEverything by creating(DefaultTask::class) {
        group = "generate"
        description = "Generates protocol models."
        dependsOn(generateDemoModel, generateInterningTestModel, generateCppTestEntities, generateTestModel)
    }
}
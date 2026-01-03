import com.jetbrains.rd.gradle.plugins.applyKotlinJVM
import com.jetbrains.rd.gradle.tasks.RdGenerateTask
import com.jetbrains.rd.gradle.tasks.util.cppDirectorySystemPropertyKey
import com.jetbrains.rd.gradle.tasks.util.csDirectorySystemPropertyKey
import com.jetbrains.rd.gradle.tasks.util.ktDirectorySystemPropertyKey

applyKotlinJVM()
plugins {
    kotlin("jvm")
}

val repoRoot: File by rootProject.extra.properties
val cppRoot: File by rootProject.extra.properties
val ktRoot: File by rootProject.extra.properties
val csRoot: File by rootProject.extra.properties

val BUILD_DIR = rootProject!!.buildDir

dependencies {
    implementation(project(":rd-gen"))
}

tasks {
    fun RdGenerateTask.prepareOutputs() {
        fun mapSources(properties: Map<String, String>, sourcesFolder: String) {
            addOutputDirectories(properties.mapKeys { "${it.key}.$sourcesFolder" })
        }

        mapSources(mapOf(
                cppDirectorySystemPropertyKey to "${cppRoot}/demo",
                ktDirectorySystemPropertyKey to "${BUILD_DIR}/models/demo",
                csDirectorySystemPropertyKey to "${csRoot}/Test.Cross/obj/DemoModel"
        ), "demo")

        mapSources(mapOf(
                cppDirectorySystemPropertyKey to "$cppRoot/src/rd_framework_cpp/src/test/util/interning",
                ktDirectorySystemPropertyKey to "$BUILD_DIR/models/interning"
        ), "interning")

        mapSources(mapOf(
                cppDirectorySystemPropertyKey to "$cppRoot/src/rd_framework_cpp/src/test/util/entities"
        ), "entities")

        mapSources(mapOf(
                ktDirectorySystemPropertyKey to "$BUILD_DIR/models/sync",
                csDirectorySystemPropertyKey to "${csRoot}/Test.Cross/obj/SyncModel"
        ), "sync")

        mapSources(mapOf(
                ktDirectorySystemPropertyKey to "$BUILD_DIR/models/openEntity"
        ), "openEntity")

        mapSources(mapOf(
                csDirectorySystemPropertyKey to "${csRoot}/Test.RdFramework/Reflection/data/Generated"
        ), "reflectionTest")
    }

    @Suppress("UNUSED_VARIABLE")
    val generateEverything by registering(RdGenerateTask::class) {
        val modelClassPath = sourceSets["main"].runtimeClasspath 
        classpath(modelClassPath)
        
        inputs.files(modelClassPath)
        
        prepareOutputs()

        args = listOf("--packages=com,org,testModels", "-v")
    }
}
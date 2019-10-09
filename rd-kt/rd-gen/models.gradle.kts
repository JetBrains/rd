val repoRoot: File by rootProject.extra.properties
val cppRoot : File by rootProject.extra.properties
val ktRoot : File by rootProject.extra.properties
val csRoot : File by rootProject.extra.properties

val BUILD_DIR = parent!!.buildDir

tasks {
    val modelsRelativePath = "rd-gen/src/models/kotlin/com/jetbrains/rd/models"

    val generateDemoModel by creating(GenerateTask::class) {
        initializeClasspath()

        sourcesRoot = ktRoot.resolve(modelsRelativePath)
        sourcesFolder = "demo"

        systemProperties = mapOf(
                "model.out.src.cpp.dir" to "$cppRoot/demo",
                "model.out.src.kt.dir" to "$BUILD_DIR/models/demo",
                "model.out.src.cs.dir" to "$csRoot/CrossTest/Model"
        )

        lateInit()
    }

    val generateInterningTestModel by creating(GenerateTask::class) {
        initializeClasspath()

        sourcesRoot = ktRoot.resolve(modelsRelativePath)
        sourcesFolder = "interning"

        systemProperties = mapOf(
                "model.out.src.cpp.dir" to "$cppRoot/src/rd_framework_cpp/src/test/util/interning",
                "model.out.src.kt.dir" to "$BUILD_DIR/models/interning"
//            "model.out.src.cs.dir" : "$csRoot/"
        )

        lateInit()
    }

    val generateTestModel by creating(GenerateTask::class) {
        initializeClasspath()

        sourcesRoot = ktRoot.resolve(modelsRelativePath)
        sourcesFolder = "test"

        systemProperties = mapOf(
                "model.out.src.kt.dir" to "$BUILD_DIR/models/test"
        )

        lateInit()
    }

    val generateCppTestEntities by creating(GenerateTask::class) {
        initializeClasspath()

        sourcesRoot = ktRoot.resolve(modelsRelativePath)
        sourcesFolder = "entities"

        systemProperties = mapOf(
                "model.out.src.cpp.dir" to "$cppRoot/src/rd_framework_cpp/src/test/util/entities"
        )

        lateInit()
    }

    val generateEverything by creating(DefaultTask::class) {
        group = "generate"
        description = "Generates protocol models."
        dependsOn(generateDemoModel, generateInterningTestModel, generateCppTestEntities, generateTestModel)
    }
}

fun GenerateTask.initializeClasspath() {
    classpath = project.the<SourceSetContainer>()["main"]!!.runtimeClasspath
}
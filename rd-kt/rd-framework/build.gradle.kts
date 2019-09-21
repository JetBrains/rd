import com.jetbrains.rd.gradle.plugins.applyMultiplatform
import com.jetbrains.rd.gradle.tasks.CopySourcesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    kotlin("multiplatform")
    java
}

applyMultiplatform()

lateinit var crossTest: SourceSet

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":rd-core"))
            }
        }
        commonTest {

        }

    }
}

kotlin.sourceSets.forEach {
    println("CERR:${it.name}")
}

sourceSets {
    crossTest = create("crossTest") {
//        compileClasspath += sourceSets.main.output + configurations.testRuntimeClasspath
//        runtimeClasspath += output + compileClasspath
    }
}

val testCopySources by tasks.creating(CopySourcesTask::class) {
    println("CERR:TASK")
    dependsOn(":rd-gen:generateEverything")
    currentSourceSet = kotlin.sourceSets.commonTest.get()
    currentProject = project
    generativeSourceSet = evaluationDependsOn(":rd-gen").sourceSets["models"]

    lateInit()
}

kotlin.sourceSets.commonTest.get().kotlin.srcDirs("C:\\Work\\rd\\rd-kt\\rd-framework\\build\\generated\\interning",
        "C:\\Work\\rd\\rd-kt\\rd-framework\\build\\generated\\demo")
testCopySources.outputs.files.forEach {
    println("FILE:$it")
}
//kotlin.sourceSets["commonTest"].kotlin.srcDirs(testCopySources.outputs.files)

tasks.named("compileTestKotlinJvm") {
    dependsOn(testCopySources)
}
//configurations[crossTest.name].extendsFrom(configurations.compileOnly.get())

/*
dependencies {
crossTestCompile sourceSets.main.output
    crossTestCompile configurations.testCompile
    crossTestCompile sourceSets.main.output
    crossTestRuntimeOnly configurations.testRuntimeOnly*//*



    crossTest.apply {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("junit:junit:${ext["junit_version"]}")
        implementation("org.jetbrains.kotlin:kotlin-test")
        implementation("org.jetbrains.kotlin:kotlin-test-junit")
    }

    val output = evaluationDependsOn(":rd-gen").sourceSets["models"].output
    output.dirs.forEach {
        print(it)
    }
    crossTest.apply {
        compile(output.dirs)
    }

}

tasks.withType<KotlinCompile> {
    //todo test
    dependsOn(":rd-gen:generateEverything")
}
//compileTestKotlin
*/

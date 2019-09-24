import com.jetbrains.rd.gradle.plugins.applyMultiplatform
import com.jetbrains.rd.gradle.tasks.CopySourcesTask

plugins {
    kotlin("multiplatform")
}

repositories {

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

sourceSets {
    crossTest = create("crossTest") {
//        compileClasspath += sourceSets.main.output + configurations.testRuntimeClasspath
//        runtimeClasspath += output + compileClasspath
    }
}

val testCopySources by tasks.creating(CopySourcesTask::class) {
    dependsOn(":rd-gen:generateEverything")
    currentSourceSet = kotlin.sourceSets.commonTest.get()
    currentProject = project
    generativeSourceSet = evaluationDependsOn(":rd-gen").sourceSets["models"]

    lateInit()
}

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

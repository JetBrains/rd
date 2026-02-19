import com.jetbrains.rd.gradle.dependencies.kotlinVersion
import com.jetbrains.rd.gradle.plugins.applyKotlinJVM
import com.jetbrains.rd.gradle.tasks.creatingCopySourcesTask

applyKotlinJVM()

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":rd-core"))
    implementation(gradleApi())
    implementation("org.jetbrains.intellij.deps:trove4j:1.0.20200330")
    testImplementation(project(":rd-framework"))
    testImplementation("org.jetbrains:annotations:26.1.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
}

sourceSets {
    create("gradlePlugin") {
        compileClasspath += sourceSets["main"].compileClasspath - files(gradle.gradleHomeDir?.resolve("lib")?.listFiles()?.filter { it.name.contains("kotlin-stdlib") || it.name.contains("kotlin-reflect") } ?: listOf<File>())
    }
}

val testCopySources by creatingCopySourcesTask(
    kotlin.sourceSets.test,
    evaluationDependsOn(":rd-gen:models").tasks.named("generateEverything")
)

tasks.named("compileTestKotlin") {
    dependsOn(testCopySources)
}

tasks {
    jar {
        from(sourceSets["gradlePlugin"].output)
    }
}

publishing.publications.named<MavenPublication>("pluginMaven") {
    pom {
        name.set("rd-gen")
        description.set("Code generator for the RD protocol.")
    }
}

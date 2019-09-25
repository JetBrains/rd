package com.jetbrains.rd.gradle.plugins

import com.jetbrains.rd.gradle.dependencies.kotlinVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

fun Project.applyKotlinJS() = apply<KotlinJSPlugin>()

@Suppress("UNUSED_VARIABLE")
class KotlinJSPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
        apply(plugin = "kotlin-platform-js")
        apply(plugin = "maven-publish")

        dependencies {
            compile("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
            testCompile("org.jetbrains.kotlin:kotlin-test-js:$kotlinVersion")
        }

        val target = "${projectDir}/build/classes/main"
        val projectName = name
        configure<KotlinMultiplatformExtension> {
            tasks {
                val sourcesJar = create<Jar>("sourcesJar") {
                    from(sourceSets["main"].kotlin.files)
                }

                val compileKotlin2Js by getting(Kotlin2JsCompile::class)
                compileKotlin2Js.apply {
                    kotlinOptions.metaInfo = true
                    kotlinOptions.outputFile = "$target/${projectName}.js"
                    kotlinOptions.sourceMap = true
                    kotlinOptions.moduleKind = "commonjs"
                    kotlinOptions.main = "call"
                    kotlinOptions.typedArrays = true
                }

                val compileTestKotlin2Js by getting(Kotlin2JsCompile::class)
                compileTestKotlin2Js.apply {
                    kotlinOptions.metaInfo = true
                    kotlinOptions.outputFile = "$target/${projectName}.test.js"
                    kotlinOptions.sourceMap = true
                    kotlinOptions.moduleKind = "commonjs"
                    kotlinOptions.main = "call"
                    kotlinOptions.typedArrays = true
                }
            }
            experimental.coroutines = Coroutines.ENABLE
        }
    }
}
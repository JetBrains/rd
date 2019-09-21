package com.jetbrains.rd.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import com.moowork.gradle.node.NodeExtension
import org.gradle.kotlin.dsl.configure

fun Project.configureNodePlugin() = apply<NodeConfigurationPlugin>()

open class NodeConfigurationPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
        apply(plugin = "com.moowork.node")

        configure<NodeExtension> {

            // Version of node to use.
            version = "8.7.0"

            // Version of npm to use.
            //npmVersion = "3.10.8"

            // Version of yarn to use.
            yarnVersion = "1.2.1"

            // Base URL for fetching node distributions (change if you have a mirror).
            distBaseUrl = "https://nodejs.org/dist"

            // If true, it will download node using above parameters.
            // If false, it will try to use globally installed node.
            download = true

            // Set the work directory for unpacking node
            workDir = file("${rootProject.buildDir}/nodejs")

            // Set the work directory where node_modules should be located
            nodeModulesDir = file("${rootProject.projectDir}")
        }

    }
}
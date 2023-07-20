package com.jetbrains.rd.generator.gradle

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
import java.util.stream.Collectors

@Deprecated("Use RdGenExtension instead", replaceWith = ReplaceWith("RdGenExtension"))
typealias RdgenParams = RdGenExtension

open class RdGenExtension(private val project: Project) {
    constructor(task: Task): this(task.project)

    fun mergeWith(defaults: RdGenExtension): RdGenExtension {
        val result = RdGenExtension(project)
        result.sources(mergeFiles(defaults) { it.sources })
        result.hashFolder = mergeObject(defaults) { it.hashFolder }
        result.compiled = mergeObject(defaults) { it.compiled }
        result.classpath(mergeFiles(defaults) { it.classpath })
        result.packages = mergeObject(defaults) { it.packages }
        result.filter = mergeObject(defaults) { it.filter }
        result.force = mergeObject(defaults) { it.force }
        result.verbose = mergeObject(defaults) { it.verbose }
        result.lineNumbersInComments = mergeObject(defaults) { it.lineNumbersInComments }
        result.clearOutput = mergeObject(defaults) { it.clearOutput }
        result.generators.addAll(mergeObject(defaults) { it.generators } ?: emptyList())
        return result
    }

    private fun <T> mergeObject(defaults: RdGenExtension?, getter: (RdGenExtension) -> T): T? {
        val value: T? = getter(this)
        if (defaults == null) return value
        if (value == null) return getter(defaults)
        if (value is Collection<*> && (value as Collection<*>).isEmpty()) return getter(defaults)
        return if (value is Map<*, *> && (value as Map<*, *>).isEmpty()) getter(defaults) else value
    }

    private fun mergeFiles(global: RdGenExtension, getter: (RdGenExtension) -> List<*>): Set<File> {
        val mergedFiles = mergeObject(global, getter)!!
        return project.files(mergedFiles).files
    }

    fun toArguments(generatorsFile: File?): List<String?> {
        val arguments = ArrayList<String?>()
        if (sourceFiles.isNotEmpty()) {
            arguments.add("-s")
            arguments.add(java.lang.String.join(";", sourceFiles))
        }
        if (hashFolder != null) {
            arguments.add("-h")
            arguments.add(hashFolder)
        }
        if (compiled != null) {
            arguments.add("--compiled")
            arguments.add(compiled)
        }
        if (classPathEntries.any()) {
            arguments.add("-c")
            arguments.add(java.lang.String.join(System.getProperty("path.separator"), classPathEntries))
        }

        if (force == true) arguments.add("-f")
        if (clearOutput == true) arguments.add("-x")
        if (packages != null) {
            arguments.add("-p")
            arguments.add(packages)
        }
        if (filter != null) {
            arguments.add("--filter")
            arguments.add(filter)
        }
        if (verbose == true) arguments.add("-v")
        val generateLineNumbersInComments = lineNumbersInComments ?: true
        if (!generateLineNumbersInComments) arguments.add("--no-line-numbers")
        if (generators.isNotEmpty()) {
            generatorsFile ?: error("generatorsFile should be passed if generators collection is not empty")
            fillGeneratorsFile(generatorsFile)

            arguments.add("-g")
            arguments.add(generatorsFile.path)
        }
        return arguments
    }

    var hashFolder: String? = null
    var compiled: String? = null
    var force: Boolean? = null
    var clearOutput: Boolean? = null
    var packages: String? = null
    var filter: String? = null
    var verbose: Boolean? = null
    var lineNumbersInComments: Boolean? = null

    private val generators: MutableList<GradleGenerationSpec> = ArrayList()
    val hasGenerators: Boolean
        get() = generators.isNotEmpty()
    fun generator(closure: Closure<GradleGenerationSpec>) = GradleGenerationSpec().apply {
        project.configure(this, closure)
        generators.add(this)
    }
    fun generator(closure: GradleGenerationSpec.() -> Unit) = GradleGenerationSpec().apply {
        closure()
        generators.add(this)
    }

    private val sources: MutableList<Any?> = ArrayList()
    fun sources(vararg paths: Any?) {
        sources.addAll(paths)
    }

    private val classpath: MutableList<Any?> = ArrayList()
    fun classpath(vararg paths: Any?) {
        classpath.addAll(paths)
    }

    private val sourceFiles: List<String>
        get() = project.files(sources).files.stream().map { obj: File -> obj.path }.collect(Collectors.toList())
    private val classPathEntries: List<String>
        get() = project.files(classpath).files.stream().map { obj: File -> obj.path }.collect(Collectors.toList())

    private fun fillGeneratorsFile(file: File) {
        val sb = StringBuilder()
        for (generator in generators) {
            sb.append(generator.toString())
            sb.append("\n")
        }
        file.writeText(sb.toString())
    }
}
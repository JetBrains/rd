package com.jetbrains.rd.gradle.tasks

open class CppBuildTask : RunScriptTask() {
    init {
        addInputs()
//        addOutputs()
    }

    private fun addInputs() {
        val excludedFolder = listOf(
            ".idea",
            ".gradle",
            "build",
            "cmake-build-debug",
            "cmake-build-release",
            "doc",
            "export")
        val includedExtensions = listOf("h", "hpp", "cpp")
        val includedFiles = listOf("CMakeLists.txt")
        val search = workingDir.walk()
            .onEnter { a -> a.name !in excludedFolder }
            .filter { a -> a.isFile && (a.extension in includedExtensions || a.name in includedFiles)}
        search.forEach { inputs.file(it) }
    }

    private fun addOutputs() {
        val includedExtensions = listOf("dll", "exe", "a", "lib")
        val search = workingDir.resolve("build").walk()
            .filter { a -> a.isFile && a.extension in includedExtensions }
        search.forEach { outputs.file(it) }
    }
}
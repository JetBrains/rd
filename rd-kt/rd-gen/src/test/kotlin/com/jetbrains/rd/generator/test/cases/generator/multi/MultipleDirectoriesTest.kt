package com.jetbrains.rd.generator.test.cases.generator.multi

import com.jetbrains.rd.generator.gradle.GradleGenerationSpec
import com.jetbrains.rd.generator.nova.IGenerator
import com.jetbrains.rd.generator.nova.Root
import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.generateRdModel
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.setting
import org.junit.Test
import java.io.File
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultipleDirectoriesTest {
    companion object {
        private val generatedSourceDirBase = File("build/testOutput/${MultipleDirectoriesTest::class.simpleName}")
        val classloader: ClassLoader = MultipleDirectoriesTest::class.java.classLoader

        private fun multiplyDirectories(file: File, n: Int): List<File> {
            return (1..n).map { file.resolve("dir$it") }
        }

        val expectedFiles = mutableListOf<File>()
        val expectedCppDirectories = mutableListOf<File>()
    }

    object MultiRoot : Root()

    object MultiRootWithSettings : Root() {
        init {
            fun uniqueName(it: IGenerator) = it::class.simpleName!!

            fun uniqueSubdirectory(it: IGenerator) =
                    generatedSourceDirBase.resolve(uniqueName(it))


            fun uniqueFile(parent: File, gen: IGenerator, newExt: String): File {
                fun uniqueFileName(gen: IGenerator, newExt: String) = uniqueName(gen) + "." + newExt

                return with(expectedFiles) {
                    parent.resolve(uniqueFileName(gen, newExt)).let {
                        add(it)
                        it
                    }
                }
            }

            setting(Kotlin11Generator.FsPath) { gen ->
                uniqueFile(uniqueSubdirectory(gen), gen, "kt")
            }

            setting(CSharp50Generator.FsPath) { gen ->
                uniqueFile(uniqueSubdirectory(gen), gen, "cs")
            }

            setting(Cpp17Generator.FsPath) {
                uniqueSubdirectory(it)
            }

            setting(Kotlin11Generator.FsPaths) { gen ->
                multiplyDirectories(uniqueSubdirectory(gen), 3).map { file ->
                    uniqueFile(file, gen, "kt")
                }
            }

            setting(CSharp50Generator.FsPaths) { gen ->
                multiplyDirectories(uniqueSubdirectory(gen), 2).map { file ->
                    uniqueFile(file, gen, "cs")
                }
            }

            setting(Cpp17Generator.FsPaths) { gen ->
                multiplyDirectories(uniqueSubdirectory(gen), 4).map {
                    uniqueSubdirectory(gen).let {
                        expectedCppDirectories.add(it)
                        it
                    }
                }
            }
        }
    }

    @Test
    fun test() {
        val files = generateRdModel(classloader,
                arrayOf("com.jetbrains.rd.generator.test.cases.generator.multi"),
                true,
                clearOutputFolderIfExists = true,
                gradleGenerationSpecs = listOf(
                        gradleGenerationSpecFactory("kotlin", 2, MultiRoot::class),
                        gradleGenerationSpecFactory("csharp", 3, MultiRoot::class),
                        gradleGenerationSpecFactory("cpp", 4, MultiRoot::class)
                )
        )
        assertEquals(2 + 3 + 4, files.size)
    }

    @Test
    fun settingsTest() {
        val files = generateRdModel(classloader,
                arrayOf("com.jetbrains.rd.generator.test.cases.generator.multi"),
                true,
                clearOutputFolderIfExists = true,
                gradleGenerationSpecs = listOf(
                        gradleGenerationSpecFactory("kotlin", 1, MultiRootWithSettings::class),
                        gradleGenerationSpecFactory("csharp", 1, MultiRootWithSettings::class),
                        gradleGenerationSpecFactory("cpp", 1, MultiRootWithSettings::class)
                )
        )
        assertEquals(3, files.size)

        expectedFiles.forEach {
            assertTrue { it.exists() }
        }

        expectedCppDirectories.forEach {
            assertTrue { it.exists() }
        }
    }

    private fun gradleGenerationSpecFactory(language: String, n: Int, rootKClass: KClass<out Root>): GradleGenerationSpec {
        return GradleGenerationSpec(
                language = language,
                root = rootKClass.qualifiedName!!,
                namespace = "org.$language",
                directory = multiplyDirectories(generatedSourceDirBase.resolve(language), n).joinToString(separator = ";")
        )
    }
}
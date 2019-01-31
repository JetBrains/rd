package com.jetbrains.rd.generator.test.cases.generator

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.util.reflection.scanForResourcesContaining
import org.junit.Assert
import org.junit.Test
import java.io.File



class InterningModelsGenTest {
    companion object {
        val kotlinGeneratedSourcesDir = "build/testOutputKotlin"
    }

    @Suppress("unused")
    object InterningRoot1 : Root(
        Kotlin11Generator(FlowTransform.AsIs, "com.jetbrains.rd.framework.test.cases.interning", File(kotlinGeneratedSourcesDir)),
        CSharp50Generator(FlowTransform.AsIs, "JetBrains.Platform.Tests.Cases.RdFramework.Interning", File("build/testOutputCSharp"))
    ) {
        val TestInternScope = internScope()

        val InterningTestModel = classdef {
            internRoot(TestInternScope)

            field("searchLabel", PredefinedType.string)
            map("issues", PredefinedType.int, structdef("WrappedStringModel") {
                field("text", PredefinedType.string.interned(TestInternScope))
            })
        }

        val InterningNestedTestModel = structdef {
            field("value", PredefinedType.string)
            field("inner", this.interned(TestInternScope).nullable)
        }

        val InterningNestedTestStringModel = structdef {
            field("value", PredefinedType.string.interned(TestInternScope))
            field("inner", this.nullable)
        }

        val InterningProtocolLevelModel = classdef {
            field("searchLabel", PredefinedType.string)
            map("issues", PredefinedType.int, structdef("ProtocolWrappedStringModel") {
                field("text", PredefinedType.string.interned(ProtocolInternScope))
            })
        }

        val InterningMtModel = classdef {
            internRoot(TestInternScope)

            field("searchLabel", PredefinedType.string)
            signal("signaller", PredefinedType.string.interned(TestInternScope)).async
            signal("signaller2", PredefinedType.string.interned(TestInternScope)).async
        }

        val InternScopeOutOfExt = internScope()
        val InterningExtensionHolder = classdef {
            internRoot(InternScopeOutOfExt)
        }


    }

    @Suppress("Unused")
    object InterningExt : Ext(InterningRoot1.InterningExtensionHolder) {
        val InternScopeInExt = internScope()

        init {
            property("root", classdef("InterningExtRootModel") {
                internRoot(InternScopeInExt)

                property("internedLocally", PredefinedType.string.interned(InternScopeInExt))
                property("internedExternally", PredefinedType.string.interned(InterningRoot1.InternScopeOutOfExt))
                property("internedInProtocol", PredefinedType.string.interned(ProtocolInternScope))
            })
        }
    }

    val classloader: ClassLoader = InterningModelsGenTest::class.java.classLoader

    @Test
    fun test1() {
        val files = generateRdModel(classloader, arrayOf("com.jetbrains.rd.generator.test.cases.generator"), true)
        assert(!files.isEmpty()) { "No files generated, bug?" }

        val rdgen = RdGen()

        val rdFrameworkClasspath = classloader.scanForResourcesContaining("com.jetbrains.rd.framework") +
                classloader.scanForResourcesContaining("com.jetbrains.rd.util")
        rdgen.classpath *= rdFrameworkClasspath.joinToString(File.pathSeparator)

        val generatedSources = File(kotlinGeneratedSourcesDir).walk().toList()
        val compiledClassesLoader = rdgen.compileDsl(generatedSources)
        Assert.assertNotNull("Failed to compile generated sources: ${rdgen.error}", compiledClassesLoader)
    }
}


package com.jetbrains.rd.generator.test.cases.generator

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.util.UsedImplicitly
import com.jetbrains.rd.util.reflection.scanForResourcesContaining
import com.jetbrains.rd.util.reflection.toPath
import org.junit.Assert
import org.junit.Test
import java.io.File

val TestInternKey = InternRootKey("Test")

class InterningModelsGenTest {
    companion object {
        val kotlinGeneratedSourcesDir = "build/testOutputKotlin"
    }


    @Suppress("unused")
    object InterningRoot1 : Root(
        Kotlin11Generator(FlowTransform.AsIs, "com.jetbrains.rd.framework.test.cases.interning", File(kotlinGeneratedSourcesDir)),
        CSharp50Generator(FlowTransform.AsIs, "JetBrains.Platform.Tests.Cases.RdFramework.Interning", File("build/testOutputCSharp"))
    ) {
        val InterningTestModel = classdef {
            internRoot(TestInternKey)

            field("searchLabel", PredefinedType.string)
            map("issues", PredefinedType.int, structdef("WrappedStringModel") {
                field("text", PredefinedType.string.interned(TestInternKey))
            })
        }

        val InterningNestedTestModel = structdef {
            field("value", PredefinedType.string)
            field("inner", this.interned(TestInternKey).nullable)
        }

        val InterningNestedTestStringModel = structdef {
            field("value", PredefinedType.string.interned(TestInternKey))
            field("inner", this.nullable)
        }

        val InterningProtocolLevelModel = classdef {
            field("searchLabel", PredefinedType.string)
            map("issues", PredefinedType.int, structdef("ProtocolWrappedStringModel") {
                field("text", PredefinedType.string.interned(ProtocolInternRoot))
            })
        }

        val InterningMtModel = classdef {
            internRoot(TestInternKey)

            field("searchLabel", PredefinedType.string)
            signal("signaller", PredefinedType.string.interned(TestInternKey)).async
        }

        val InternKeyOutOfExt = InternRootKey("OutOfExt")
        val InterningExtensionHolder = classdef {
            internRoot(InternKeyOutOfExt)
        }


    }

    @Suppress("Unused")
    object InterningExt : Ext(InterningRoot1.InterningExtensionHolder) {
        val InternKeyInExt = InternRootKey("InExt")

        init {
            property("root", classdef("InterningExtRootModel") {
                internRoot(InternKeyInExt)

                property("internedLocally", PredefinedType.string.interned(InternKeyInExt))
                property("internedExternally", PredefinedType.string.interned(InterningRoot1.InternKeyOutOfExt))
                property("internedInProtocol", PredefinedType.string.interned(ProtocolInternRoot))
            })
        }
    }

    val classloader: ClassLoader = InterningModelsGenTest::class.java.classLoader

    @Test
    fun test1() {
        generateRdModel(classloader, arrayOf("com.jetbrains.rider.generator.test.cases.generator"), true)

        val rdgen = RdGen()

        val rdFrameworkClasspath = classloader.scanForResourcesContaining("com.jetbrains.rider.framework") +
                classloader.scanForResourcesContaining("com.jetbrains.rider.util")
        rdgen.classpath *= rdFrameworkClasspath.joinToString(File.pathSeparator)

        val generatedSources = File(kotlinGeneratedSourcesDir).walk().toList()
        val compiledClassesLoader = rdgen.compileDsl(generatedSources)
        Assert.assertNotNull("Failed to compile generated sources: ${rdgen.error}", compiledClassesLoader)
    }
}


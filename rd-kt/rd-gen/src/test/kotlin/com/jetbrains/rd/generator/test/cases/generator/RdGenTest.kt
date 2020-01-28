package com.jetbrains.rd.generator.test.cases.generator

import com.jetbrains.rd.generator.gradle.GradleGenerationSpec
import com.jetbrains.rd.generator.nova.RdGen
import java.io.File
import java.net.URLClassLoader


enum class Configuration {
    EXAMPLE,
    DEMO_MODEL,
    RIDER_MODEL,
    ENTITY_MODEL,
    UNREAL_MODEL
}


fun main() {
    val rdgen = RdGen()
    rdgen.verbose *= true
//    rdgen.force *= true
    rdgen.clearOutput *= true
//    rdgen.filter *= "cpp"
//    rdgen.filter *= "cpp|csharp"
//    rdgen.filter *= "kotlin"
    val configuration = Configuration.UNREAL_MODEL
    when (configuration) {
        Configuration.EXAMPLE -> {
            rdgen.sources *= "C:\\Work\\rd\\rd-gen\\src\\test\\kotlin\\com\\jetbrains\\rd\\generator\\test\\cases\\generator\\example"
        }
        Configuration.DEMO_MODEL -> {
            System.setProperty("model.out.src.cpp.dir", "C:\\Work\\rd\\rd-cpp\\demo")
            System.setProperty("model.out.src.kt.dir", "C:\\Work\\rd\\rd-kt\\rd-gen\\build\\models\\demo")
            System.setProperty("model.out.src.cs.dir", "C:\\Work\\rd\\rd-net\\Test.RdGen\\CrossTest\\Model")

            rdgen.sources *= "C:\\Work\\rd\\rd-kt\\rd-gen\\src\\test\\kotlin\\com\\jetbrains\\rd\\generator\\test\\cases\\generator\\demo"
            rdgen.packages *= "com.jetbrains.rd.generator.test.cases.generator.demo"
        }
        Configuration.RIDER_MODEL -> {
            System.setProperty("model.out.src.cpp.dir", "C:\\Work\\rd\\rd-cpp\\rider_model")
            System.setProperty("model.out.src.kt.dir", "C:\\Work\\rd\\ide-model")

            rdgen.sources *= "C:\\Work\\ide-model"
            rdgen.packages *= "com.jetbrains.rider.model.nova.ide"
        }
        Configuration.ENTITY_MODEL -> {
            System.setProperty("model.out.src.cpp.dir", "C:\\Work\\rd\\rd-cpp\\src\\rd_framework_cpp\\src\\test\\util\\entities")

            rdgen.sources *= "C:\\Work\\rd\\rd-gen\\src\\test\\kotlin\\com\\jetbrains\\rd\\generator\\test\\cases\\generator\\entities"
            rdgen.packages *= "com.jetbrains.rd.generator.test.cases.generator.entities"
        }
        Configuration.UNREAL_MODEL -> {
            System.setProperty("model.out.src.lib.ue4.csharp.dir", "C:\\Work\\resharper-unreal\\src\\dotnet\\ReSharperPlugin.resharper_unreal\\obj\\model\\Library")
            System.setProperty("model.out.src.lib.ue4.cpp.dir", "C:\\Work\\resharper-unreal\\src\\cpp\\RiderLink\\Source\\RiderLink\\Private\\RdEditorProtocol")
            System.setProperty("model.out.src.lib.ue4.kt.dir", "C:\\Work\\resharper-unreal\\src\\rider\\main\\kotlin\\com\\jetbrains\\rider\\model\\RdRiderProtocol")

            System.setProperty("model.out.src.editorPlugin.csharp.dir", "C:\\Work\\resharper-unreal\\src\\dotnet\\ReSharperPlugin.resharper_unreal\\obj\\model\\RdEditorProtocol")
            System.setProperty("model.out.src.editorPlugin.cpp.dir", "C:\\Work\\resharper-unreal\\src\\cpp\\RiderLink\\Source\\RiderLink\\Private\\RdEditorProtocol")

            System.setProperty("model.out.src.rider.csharp.dir", "C:\\Work\\resharper-unreal\\src\\dotnet\\ReSharperPlugin.resharper_unreal\\obj\\model\\RdRiderProtocol")
            System.setProperty("model.out.src.rider.kotlin.dir", "C:\\Work\\resharper-unreal\\src\\rider\\main\\kotlin\\com\\jetbrains\\rider\\model\\RdRiderProtocol")

            rdgen.sources *=
                "C:\\Work\\resharper-unreal\\protocol\\src\\main\\kotlin\\model\\editorPlugin" + ";" +
                    "C:\\Work\\resharper-unreal\\protocol\\src\\main\\kotlin\\model\\lib\\ue4" + ";" +
                    "C:\\Work\\resharper-unreal\\protocol\\src\\main\\kotlin\\model\\rider"
            rdgen.packages *= "model.editorPlugin" + ";" +
                "model.lib.ue4" + ";" +
                "model.rider"

            rdgen.gradleGenerationSpecs.add(GradleGenerationSpec(
                language = "kotlin",
                transform = "asis",
                root = "com.jetbrains.rider.model.nova.ide.IdeRoot",
                namespace = "com.jetbrains.rider.model",
                directory = "C:\\Work\\resharper-unreal\\src\\rider\\main\\kotlin\\com\\jetbrains\\rider\\model\\RdRiderProtocol"
            ))

            rdgen.gradleGenerationSpecs.add(GradleGenerationSpec(
                language = "csharp",
                transform = "reversed",
                root = "com.jetbrains.rider.model.nova.ide.IdeRoot",
                namespace = "JetBrains.Rider.Model",
                directory = "C:\\Work\\resharper-unreal\\src\\dotnet\\ReSharperPlugin.resharper_unreal\\obj\\model\\RdRiderProtocol"
            ))

            rdgen.classpath *= "C:\\Work\\resharper-unreal\\build\\riderRD-2019.3-SNAPSHOT\\lib\\rd\\rider-model.jar"
        }
    }
    rdgen.compilerClassloader = URLClassLoader(arrayOf(
        File("C:\\RD\\dotnet-products\\Rider\\ultimate\\community\\build\\dependencies\\build\\kotlin\\Kotlin\\kotlinc\\lib\\kotlin-compiler.jar").toURI().toURL()
    ))

    rdgen.run()
}

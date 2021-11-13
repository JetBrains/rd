package com.jetbrains.rd.generator.test.cases.generator

import com.jetbrains.rd.generator.nova.RdGen
import java.io.File
import java.net.URLClassLoader

enum class Configuration {
    EXAMPLE,
    DEMO_MODEL,
    RIDER_MODEL,
    ENTITY_MODEL
}

fun main() {
    val rdgen = RdGen().apply { verbose *= true }
    rdgen.verbose *= true
//    rdgen.force *= true
    rdgen.clearOutput *= true
//    rdgen.filter *= "cpp"
//    rdgen.filter *= "cpp|csharp"
//    rdgen.filter *= "kotlin"
    val configuration = Configuration.DEMO_MODEL
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
    }
    rdgen.compilerClassloader = URLClassLoader(arrayOf(
        File("C:\\Users\\jetbrains\\.IntelliJIdea2018.2\\config\\plugins\\Kotlin\\kotlinc\\lib\\kotlin-compiler.jar").toURI().toURL()
    ))

    rdgen.run()
}

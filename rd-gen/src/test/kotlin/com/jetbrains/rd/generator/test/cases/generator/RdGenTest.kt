package com.jetbrains.rd.generator.test.cases.generator

import com.jetbrains.rd.generator.nova.RdGen
import java.io.File
import java.net.URLClassLoader


enum class Configuration {
    EXAMPLE,
    DEMO_MODEL,
    RIDER_MODEL,
    INTERNING_MODEL
}


fun main() {
    val rdgen = RdGen()
    rdgen.verbose *= true
    rdgen.force *= true
    rdgen.clearOutput *= true
//    rdgen.filter *= "cpp"
//    rdgen.filter *= "kotlin"
    val configuration = Configuration.DEMO_MODEL
    when (configuration) {
        Configuration.EXAMPLE -> {
            rdgen.sources *= "C:\\Work\\rd\\rd-gen\\src\\test\\kotlin\\com\\jetbrains\\rd\\generator\\test\\cases\\generator\\example"
        }
        Configuration.DEMO_MODEL -> {
            System.setProperty("model.out.src.cpp.dir", "C:\\Work\\rd\\rd-cpp\\demo\\model")
            System.setProperty("model.out.src.kt.dir", "C:\\Work\\rd\\rd-framework\\src\\jvmTest\\kotlin\\com\\jetbrains\\rd\\framework\\test\\cases\\demo\\model")
            rdgen.sources *= "C:\\Work\\rd\\rd-gen\\src\\test\\kotlin\\com\\jetbrains\\rd\\generator\\test\\cases\\generator\\demo"
            rdgen.packages *= "com.jetbrains.rd.generator.test.cases.generator.demo"
        }
        Configuration.RIDER_MODEL -> {
            System.setProperty("model.out.src.cpp.dir", "C:\\Work\\rd\\rd-cpp\\cpp_model")
            System.setProperty("model.out.src.kt.dir", "C:\\Work\\rd\\ide-model")

            rdgen.sources *= "C:\\Work\\ide-model"
            rdgen.packages *= "com.jetbrains.rider.model.nova.ide"
        }
        Configuration.INTERNING_MODEL -> {
            System.setProperty("model.out.src.cpp.dir", "C:\\Work\\rd\\rd-cpp\\src\\rd_framework_cpp\\src\\test\\util\\models")
            System.setProperty("model.out.src.kt.dir", "C:\\Work\\rd\\rd-cpp\\src\\rd_framework_cpp\\src\\test\\util\\models")


            rdgen.sources *= "C:\\Work\\rd\\rd-gen\\src\\test\\kotlin\\com\\jetbrains\\rd\\generator\\test\\cases\\generator"
            rdgen.packages *= "com.jetbrains.rd.generator.test.cases.generator"
        }
    }
    rdgen.compilerClassloader = URLClassLoader(arrayOf(
            File("C:\\Users\\jetbrains\\.IntelliJIdea2018.2\\config\\plugins\\Kotlin\\kotlinc\\lib\\kotlin-compiler.jar").toURI().toURL()
    ))

    rdgen.run()
}
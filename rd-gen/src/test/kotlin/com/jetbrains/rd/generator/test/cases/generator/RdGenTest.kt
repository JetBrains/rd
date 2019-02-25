package com.jetbrains.rd.generator.test.cases.generator

import com.jetbrains.rd.generator.nova.RdGen
import java.io.File
import java.net.URLClassLoader


enum class Configuration {
    EXAMPLE,
    DEMO_MODEL,
    RIDER_MODEL
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
            rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\rd\\rd-gen\\src\\test\\kotlin\\com\\jetbrains\\rd\\generator\\test\\cases\\generator\\example"
        }
        Configuration.DEMO_MODEL -> {
            System.setProperty("model.out.src.cpp.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-cpp\\cpp_model")
            System.setProperty("model.out.src.kt.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-framework\\src\\jvmTest\\kotlin\\com\\jetbrains\\rd\\framework\\test\\cases\\demo")

            rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\rd\\rd-gen\\src\\test\\kotlin\\com\\jetbrains\\rd\\generator\\test\\cases\\generator\\demo"
            rdgen.packages *= "com.jetbrains.rd.generator.test.cases.generator.demo"
        }
        Configuration.RIDER_MODEL -> {
            System.setProperty("model.out.src.cpp.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-cpp\\cpp_model")
            System.setProperty("model.out.src.kt.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-framework\\src\\jvmTest\\kotlin\\com\\jetbrains\\rider\\framework\\test\\cases\\kotlin_model")

            rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\ide-model\\src\\com\\jetbrains\\rider\\model\\nova\\ide"
//            rdgen.packages *= "com.jetbrains.rider.generator.test.cases.generator.demo"
        }
    }
    rdgen.compilerClassloader = URLClassLoader(arrayOf(
            File("C:\\Users\\jetbrains\\.IntelliJIdea2018.2\\config\\plugins\\Kotlin\\kotlinc\\lib\\kotlin-compiler.jar").toURI().toURL()
    ))

    rdgen.run()
}
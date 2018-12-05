package com.jetbrains.rider.generator.test.cases.generator

import com.jetbrains.rider.generator.nova.RdGen
import java.io.File
import java.net.URLClassLoader


enum class Configuration {
    EXAMPLE,
    UNREAL_ENGINE,
    RIDER_MODEL,
    TEST_MODEL,
    DEMO_MODEL
}


fun main(args: Array<String>) {
    val rdgen = RdGen()
    rdgen.verbose *= true
    rdgen.force *= true
    rdgen.clearOutput *= true
//        rdgen.filter *= "cpp"
    System.setProperty("model.out.src.kt.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-cpp\\kt_model")
    System.setProperty("model.out.src.cpp.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-cpp\\cpp_model")
    System.setProperty("model.out.src.unrealengine.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-cpp\\cpp_model")

    val configuration = Configuration.DEMO_MODEL
    when (configuration) {
        Configuration.EXAMPLE -> {
            rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\rd\\rd-gen\\src\\test\\kotlin\\com\\jetbrains\\rider\\generator\\test\\cases\\generator\\example"
        }
        Configuration.UNREAL_ENGINE -> {
            rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\ue_model"
            rdgen.packages *= "com.jetbrains.rider.model.nova.unrealengine"
        }
        Configuration.RIDER_MODEL -> {
            rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\ide-model\\src\\com\\jetbrains\\rider\\model\\nova\\ide"
            rdgen.packages *= "com.jetbrains.rider.model.nova.ide"
        }
        Configuration.TEST_MODEL -> {
            rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\test_model"
            rdgen.packages *= "com.jetbrains.rider.model.nova.test"
        }
        Configuration.DEMO_MODEL -> {
            System.setProperty("model.out.src.kt.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-framework\\src\\jvmTest\\kotlin\\com\\jetbrains\\rider\\framework\\test\\cases\\kt_model")

            rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\rd\\rd-gen\\src\\test\\kotlin\\com\\jetbrains\\rider\\generator\\test\\cases\\generator\\demo"
            rdgen.packages *= "com.jetbrains.rider.generator.test.cases.generator.demo"
        }
    }
    rdgen.compilerClassloader = URLClassLoader(arrayOf(
            File("C:\\Users\\jetbrains\\.IntelliJIdea2018.2\\config\\plugins\\Kotlin\\kotlinc\\lib\\kotlin-compiler.jar").toURI().toURL()
    ))

    rdgen.run()
}
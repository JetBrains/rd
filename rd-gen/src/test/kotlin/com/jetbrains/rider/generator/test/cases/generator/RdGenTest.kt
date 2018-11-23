package com.jetbrains.rider.generator.test.cases.generator

import com.jetbrains.rider.generator.nova.RdGen
import org.junit.Test
import java.io.File
import java.net.URLClassLoader


class RdGenTest {

    enum class Configuration {
        EXAMPLE,
        UNREAL_ENGINE,
        RIDER_MODEL,
        TEST_MODEL
    }

    @Test
    fun testParse() {
        val rdgen = RdGen()
        rdgen.verbose *= true
        rdgen.force *= true
        rdgen.clearOutput *= true
//        rdgen.filter *= "cpp"
        val configuration = Configuration.TEST_MODEL
        when (configuration) {
            Configuration.EXAMPLE -> {
                rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\rd\\rd-gen\\src\\test\\kotlin\\com\\jetbrains\\rider\\generator\\test\\cases\\generator\\example"
                System.setProperty("model.out.src.kt.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-cpp\\kt_model")
                System.setProperty("model.out.src.cpp.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-cpp\\cpp_model")
            }
            Configuration.UNREAL_ENGINE -> {
                rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\ue_model"
                rdgen.packages *= "com.jetbrains.rider.model.nova.unrealengine"
                System.setProperty("model.out.src.kt.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-cpp\\kt_model")
                System.setProperty("model.out.src.unrealengine.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-cpp\\cpp_model")
            }
            Configuration.RIDER_MODEL -> {
                rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\ide-model\\src\\com\\jetbrains\\rider\\model\\nova\\ide"
                rdgen.packages *= "com.jetbrains.rider.model.nova.ide"
                System.setProperty("model.out.src.kt.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-cpp\\kt_model")
                System.setProperty("model.out.src.cpp.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-cpp\\cpp_model")
            }
            Configuration.TEST_MODEL -> {
                rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\test_model"
                rdgen.packages *= "com.jetbrains.rider.model.nova.test"
                System.setProperty("model.out.src.kt.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-cpp\\kt_model")
                System.setProperty("model.out.src.cpp.dir", "C:\\Users\\jetbrains\\Documents\\rd\\rd-cpp\\cpp_model")
            }
        }
        rdgen.compilerClassloader = URLClassLoader(arrayOf(
                File("C:\\Users\\jetbrains\\.IntelliJIdea2018.2\\config\\plugins\\Kotlin\\kotlinc\\lib\\kotlin-compiler.jar").toURI().toURL()
        ))

        rdgen.run()
    }
}
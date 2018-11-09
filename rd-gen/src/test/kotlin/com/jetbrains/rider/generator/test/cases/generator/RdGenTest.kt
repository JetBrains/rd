package com.jetbrains.rider.generator.test.cases.generator

import com.jetbrains.rider.generator.nova.RdGen
import org.junit.Test
import java.io.File
import java.net.URLClassLoader


class RdGenTest {

    @Test
    fun testParse() {
        val rdgen = RdGen()
        System.setProperty("model.out.src.kt.dir", "C:\\temp\\kt")
        rdgen.verbose *= true
        rdgen.force *= true
//        rdgen.sources *= "C:\\work\\Rider\\Platform\\RdProtocol\\rider-model\\Src"
//        rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\Gen"
//        rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\rd\\rd-gen\\src\\test\\kotlin\\com\\jetbrains\\rider\\generator\\test\\cases\\generator\\example"
        rdgen.sources *= "C:\\Users\\jetbrains\\Documents\\ide-model\\src\\com\\jetbrains\\rider\\model\\nova\\ide"
        rdgen.filter *= "cpp"
        rdgen.compilerClassloader = URLClassLoader(arrayOf(
//            File("C:\\Users\\dmitry.ivanov\\.IntelliJIdea2017.2\\config\\plugins\\Kotlin\\kotlinc\\lib\\kotlin-compiler.jar").toURI().toURL()
            File("C:\\Users\\jetbrains\\.IntelliJIdea2018.2\\config\\plugins\\Kotlin\\kotlinc\\lib\\kotlin-compiler.jar").toURI().toURL()
        ))

        rdgen.run()
    }
}
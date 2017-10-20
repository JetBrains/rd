package com.jetbrains.rider.generator.test.cases.generator

import com.jetbrains.rider.generator.nova.RdGen
import org.testng.annotations.Test
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths


class RdGenTest {

//    @Test
    fun testParse() {
        val rdgen = RdGen()
        System.setProperty("model.out.src.kt.dir", "c:\\temp\\kt")
        rdgen.verbose *= true
        rdgen.sources *= Paths.get("C:\\work\\Rider\\Platform\\RdProtocol\\rider-model\\Src")
        rdgen.filter *= "kotlin"
        rdgen.compilerClassloader = URLClassLoader(arrayOf(
            File("C:\\Users\\dmitry.ivanov\\.IntelliJIdea2017.2\\config\\plugins\\Kotlin\\kotlinc\\lib\\kotlin-compiler.jar").toURI().toURL()
        ))

        rdgen.run()
    }
}
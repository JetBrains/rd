package com.jetbrains.rd.generator.test.cases.util

import com.jetbrains.rd.util.ClassLoaderUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ClassLoaderUtilTest {
    @Test
    fun checkClassLoaderUrl() {
        val urls = ClassLoaderUtil.createClassLoaderWithoutClassesFromGradleDistribution().urLs
        assertTrue(urls.all { !it.toString().contains("gradle-installation-beacon") })
    }
}
package com.jetbrains.rd.util

import java.io.File
import java.net.URLClassLoader

object ClassLoaderUtil {
    fun createClassLoaderWithoutClassesFromGradleDistribution(): ClassLoader {
        val urls = (ClassLoaderUtil::class.java.classLoader as URLClassLoader).urLs
        val gradleLibDir = File(urls.single { it.toString().contains("gradle-installation-beacon") }.toURI()).parentFile
        return URLClassLoader(urls?.filterNot { File(it.toURI()).startsWith(gradleLibDir) }?.toTypedArray() ?: arrayOf(), null)
    }
}
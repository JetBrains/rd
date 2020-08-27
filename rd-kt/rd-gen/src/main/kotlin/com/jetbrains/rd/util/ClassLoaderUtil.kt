package com.jetbrains.rd.util

import java.io.File
import java.net.URLClassLoader

object ClassLoaderUtil {
    fun createClassLoaderWithoutClassesFromGradleDistribution(): URLClassLoader {
        val urls = System.getProperty("java.class.path").split(File.pathSeparatorChar)
        val gradleLibDir = File(urls.single { it.contains("gradle-installation-beacon") }).parentFile
        return URLClassLoader(urls.map{ File(it) }.filterNot { it.startsWith(gradleLibDir) }.map { it.toURI().toURL() }.toTypedArray(), null)
    }
}
package com.jetbrains.rd.util

import java.io.File
import java.net.URLClassLoader

object ClassLoaderUtil {
    fun createClassLoaderWithoutClassesFromGradleDistribution(): URLClassLoader {
        val systemClassLoader = ClassLoaderUtil::class.java.classLoader
        val urls = if (systemClassLoader is URLClassLoader) {
            systemClassLoader.urLs.toList()
        } else {
            val classpathString = System.getProperty("java.class.path")
            classpathString.split(File.pathSeparatorChar).map { File(it).toURI().toURL() }
        }
        if (urls.all { !it.toString().contains("gradle-installation-beacon")}) {
            return URLClassLoader(urls.toTypedArray(), null)
        }
        val gradleLibDir = File(urls.single { it.toString().contains("gradle-installation-beacon") }.toURI()).parentFile
        return URLClassLoader(urls.map { File(it.toURI()) }.filterNot { it.startsWith(gradleLibDir) }.map { it.toURI().toURL() }.toTypedArray(), null)
    }
}
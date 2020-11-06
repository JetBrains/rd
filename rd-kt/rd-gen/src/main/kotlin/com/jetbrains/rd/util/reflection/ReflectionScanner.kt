package com.jetbrains.rd.util.reflection

import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.util.jar.JarFile
import kotlin.system.measureTimeMillis

private fun String.pkg2path(respectOs : Boolean) = replace('.', if (respectOs) File.separatorChar else '/')
private fun String.path2pkg() = replace(File.separatorChar, '.').replace('/','.')
private val classSuffix : String = ".class"

/**
 * Finds all classes from given classloader whose full name starts from *pkgs*
 * @param pkgs
 * @return Sequence of found classes
 */
fun ClassLoader.scanForClasses(vararg pkgs: String): Sequence<Class<*>> {
    return pkgs.asSequence().flatMap { pkg ->
        getResources(pkg.pkg2path(false))
        .asSequence()
        .flatMap { url ->
           url.process(pkg,
               { file -> file.scanForClasses(pkg, this) },
               { jar -> jar.scanForClasses(pkg, this) },
               { emptySequence() }
           )
        }
    }.distinct()
}

fun ClassLoader.scanForResourcesContaining(vararg pkgs: String): Sequence<File> {
    return pkgs.asSequence().flatMap { pkg ->
        getResources(pkg.pkg2path(false))
            .asSequence()
            .flatMap { url ->
                url.process(pkg,
                    { sequenceOf(it) },
                    { jar -> sequenceOf(File(jar.name)) },
                    { emptySequence() }
                )
            }
    }.distinct()
}


private fun <T> URL.process(pkg: String, processFile: (File) -> T, processJar: (JarFile) -> T, onFail: () -> T) : T {
    return when (protocol) {
        "jar" -> {
            val path = urlDecode(toExternalForm().substringAfter("file:").substringBeforeLast("!"))
            processJar(JarFile(path))
        }
        "file" -> {
            processFile(toPath(pkg))
        }
        else -> onFail()
    }
}

fun URL.toPath(pkg: String = ""): File {
    val path = File(urlDecode(path)).absolutePath.removeSuffix(pkg.pkg2path(true))
    return File(path)
}


//private fun URL.scanForClasses(pkg: String, classLoader: ClassLoader): Sequence<Class<*>> {
//    return when (protocol) {
//        "jar" -> JarFile(urlDecode(toExternalForm().substringAfter("file:").substringBeforeLast("!"))).scanForClasses(pkg, classLoader)
//        "file" -> File(urlDecode(path)).scanForClasses(pkg, classLoader)
//        else -> emptySequence() //not supported
//    }
//}

private fun File.scanForClasses(pkg: String, classLoader: ClassLoader): Sequence<Class<*>> {
    val root = this
    return walkTopDown()
            .filter { it.isFile && it.name.endsWith(classSuffix) }
            .map {
                val classFileLocation = it.absolutePath

                val relativeToRoot = classFileLocation.removePrefix(root.toString()).removePrefix(File.separator)
                if (!relativeToRoot.startsWith(pkg.pkg2path(true))) return@map null
                val className = relativeToRoot.removeSuffix(classSuffix).path2pkg()
                val clazz = classLoader.tryLoadClass(className)
                clazz
            }.filterNotNull()
}

private fun JarFile.scanForClasses(prefix: String, classLoader: ClassLoader): Sequence<Class<*>> {
    val path = prefix.pkg2path(false) + '/'
    return entries().asSequence()
            .filter {!it.isDirectory && it.name.endsWith(classSuffix) && it.name.startsWith(path)}
            .map {
                val fqName = it.name.removeSuffix(classSuffix).path2pkg()
                classLoader.tryLoadClass(fqName)
            }.filterNotNull()
}

private fun ClassLoader.tryLoadClass(fqName: String): Class<*>? {
    try {
        return loadClass(fqName)
    } catch (e: Throwable) {
        return null
    }
}

private fun urlDecode(encoded: String): String {
    try {
        return URLDecoder.decode(encoded, "UTF-8")
    } catch(e: Exception) {
        return encoded
    }
}

private class _ReflectionScannerRulezzz

fun main(args: Array<String>) {
    val p = _ReflectionScannerRulezzz()
    val classLoader = p.javaClass.classLoader


    classLoader.scanForResourcesContaining("com.jetbrains.rd").forEach { println(it) }

    println()

    val ourPkg = "com.jetbrains"
    var time = measureTimeMillis {
        val classes = classLoader.scanForClasses(ourPkg).toList();
        println("Classes in '$ourPkg': " + classes.count())
    }
    println("$time ms")

    time = measureTimeMillis { classLoader.scanForClasses(ourPkg).toList(); }
    println("$time ms")

    time = measureTimeMillis { classLoader.scanForClasses(ourPkg).toList(); }
    println("$time ms")

}

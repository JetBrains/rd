package com.jetbrains.rider.generator.nova


//Entrance point
private val pkgProperty = "rd.model.pkg"
private val filterProperty = "rd.gen.filter"
private val verboseProperty = "rd.verbose"
private class RiderRulezzz



//inline fun err(s: String?) {
//    if (s != null) return
//}

fun main(args: Array<String>) {
    RdGen().apply {
        parse(args)
        if (!run()) System.exit(1)
    }
}




//    val pkg = System.getProperty(pkgProperty)?.split(',', ' ', ':', ';') ?: run {
//
//        System.err.println()
//        System.err.println("RD model generator, v 0.99")
//        System.err.println("Usage: java -cp [CLASSPATH] -D[PROPERTY]... ${RiderRulezzz::class.java.`package`.name}.MainKt")
//        System.err.println("Search for inheritors of '${Toplevel::class.java.name}' in given classpath and generate sources according given generators (inheritors of '${IGenerator::class.java.name}')")
//        System.err.println("")
//
//        val pad = 20
//        System.err.println("Properties:")
//        System.err.println("  ${pkgProperty.padEnd(pad)} [Mandatory] Java package names to search toplevels, delimited by ','. Example: com.jetbrains.rider.model.nova")
//        System.err.println("  ${filterProperty.padEnd(pad)} [Optional]  Filter generators by searching given regex inside generator class simple name (case insensitive). Example: kotlin")
//        System.err.println("  ${verboseProperty.padEnd(pad)} [Optional]  Verbose output about generator's actions. Values: true/false")
//
//        System.exit(1)
//        return
//    }
//
//
//    val generatorFilter = System.getProperty(filterProperty)?.let { Regex(it, RegexOption.IGNORE_CASE) }
//    val verbose = System.getProperty(verboseProperty)?.toLowerCase() == "true"
//    val classloader = RiderRulezzz::class.java.classLoader
//
//    generateRdModel(classLoader = classloader
//        , namespacePrefixes = pkg.toTypedArray()
//        , verbose = verbose
//        , generatorsFilter = generatorFilter
//    )
//}

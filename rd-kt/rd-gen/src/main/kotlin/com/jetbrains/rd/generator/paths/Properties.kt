package com.jetbrains.rd.generator.paths

import com.jetbrains.rd.generator.nova.util.syspropertyOrInvalid
import java.io.File

/**
 * Default prefixes for system properties, that're used for getting output directory in generators.
 * Last ones're passed via constructor in kotlin DSL.
 *
 * For example "model.out.src.cpp.dir.demo" is using for passing output directory to DemoModel.
 */
const val cppDirectorySystemPropertyKey = "model.out.src.cpp.dir"
const val ktDirectorySystemPropertyKey = "model.out.src.kt.dir"
const val csDirectorySystemPropertyKey = "model.out.src.cs.dir"

/**
 * Gets output directory from system properties by name concating prefix and through symbol '.'
 */
fun outputDirectory(prefix: String, suffix: String) = File(syspropertyOrInvalid("$prefix.$suffix"))
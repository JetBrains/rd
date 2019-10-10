package com.jetbrains.rd.util.paths

import com.jetbrains.rd.generator.nova.util.syspropertyOrInvalid
import java.io.File

const val cppDirectorySystemPropertyKey = "model.out.src.cpp.dir"
const val ktDirectorySystemPropertyKey = "model.out.src.kt.dir"
const val csDirectorySystemPropertyKey = "model.out.src.cs.dir"

fun outputDirectory(property: String, folder : String) = File(syspropertyOrInvalid("$property.$folder"))
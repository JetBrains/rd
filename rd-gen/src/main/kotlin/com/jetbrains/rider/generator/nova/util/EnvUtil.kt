package com.jetbrains.rider.generator.nova.util


fun syspropertyOrEmpty(name: String) : String = System.getProperty(name) ?: ""



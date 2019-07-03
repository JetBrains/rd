package com.jetbrains.rd.gradle.tasks

import org.gradle.api.Task


val Task.goldFilePath
    get() = "${project.rootProject.rootDir}/buildSrc/src/main/resources/gold/$name.gold"

val Task.tmpFilePath
    get() = "${project.rootProject.buildDir}/src/main/resources/tmp/$name.tmp"






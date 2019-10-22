package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction

/**
 * The same as JavaExec, but runs kotlin app.
 */
open class KotlinExec : JavaExec()
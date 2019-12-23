package com.jetbrains.rd.gradle.plugins

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSetContainer

val org.gradle.api.Project.`sourceSets`: SourceSetContainer get() =
    (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer


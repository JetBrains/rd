package com.jetbrains.rd.gradle.tasks

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject


@Suppress("UnstableApiUsage")
public class KotlinExec() : JavaExec(), Runnable {
    /*init {
//        classpath += project.sourceSets.test.runtimeClasspath
//        classpath += project.sourceSets.test.compileClasspath
//    classpath += project(':rd-framework:').sourceSets.test.output.classesDir
//    classpath += files("${project.rootDir}/rd-kt/rd-framework/build/classes/kotlin/jvm/test")
//    classpath += files('C:\\Work\\rd\\rd-kt\\rd-framework\\build\\classes\\kotlin\\jvm\\test\\com\\jetbrains\\rd\\framework\\test\\cross')
//        classpath += files("rd-kt\\rd-framework\\build\\classes\\kotlin\\jvm\\test")
        main = "com.jetbrains.rd.framework.test.cross.CrossTestServerAllEntitiesKt"
    }*/

    /*@Inject
    lateinit var workerExecutor: WorkerExecutor*/

    @TaskAction
    public fun action() {
        /*println("EXEC")
        println(classpath.getFiles())
        super.exec()*/
    }

    override fun run() {
        /*println("RUN")
        exec()*/
    }
}
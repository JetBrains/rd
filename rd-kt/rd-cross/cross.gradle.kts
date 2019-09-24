import com.jetbrains.rd.gradle.tasks.CrossTestKtRdTask
import com.jetbrains.rd.gradle.tasks.InteropTask

dependencies {
//    testCompileOnly files { project(":rd-framework:").sourceSets.test.kotlin.srcDirs }
//    compile project(":rd-framework:").sourceSets.test.output
    compile(project(":rd-framework:"))
    implementation(gradleApi())
}

/*
def getCppTaskByName(String name) {
    return project(":rd-cpp").getTasksByName(name, true).iterator().next()
}
*/

/*fun getCsTaskByName(name : String): Task {
    return project(":rd-net").getTasksByName(name, true).iterator().next()
}*/

//region Kt
/*task CrossTestKtServerAllEntities(type: CrossTestKtRdCall, dependsOn: tasks.getByPath(":rd-framework:jvmTestClasses")) {
    classpath += project.sourceSets.test.runtimeClasspath
    classpath += project.sourceSets.test.compileClasspath
}

task CrossTestKtServerBigBuffer(type: CrossTestKtRdCall, dependsOn: tasks.getByPath(":rd-framework:jvmTestClasses")) {
    classpath += project.sourceSets.test.runtimeClasspath
    classpath += project.sourceSets.test.compileClasspath
}

task CrossTestKtServerRdCall(type: CrossTestKtRdCall, dependsOn: tasks.getByPath(":rd-framework:jvmTestClasses")) {
    classpath += project.sourceSets.test.runtimeClasspath
    classpath += project.sourceSets.test.compileClasspath
}*/
//endregion

//region KtCpp
/*task CrossTestKtCppAllEntities(type: InteropTask) {
    taskServer = CrossTestKtServerAllEntities
    taskClient = getCppTaskByName("CrossTestCppClientAllEntities")

    addDependencies()
}

task CrossTestKtCppBigBuffer(type: InteropTask) {
    taskServer = CrossTestKtServerBigBuffer
    taskClient = getCppTaskByName("CrossTestCppClientBigBuffer")

    addDependencies()
}

task CrossTestKtCppRdCall(type: InteropTask) {
    taskServer = CrossTestKtServerRdCall
    taskClient = getCppTaskByName("CrossTestCppClientRdCall")
    
    addDependencies()
}*/
//endregion

//region KtCs
/*task CrossTestKtCsAllEntities(type: InteropTask) {
    taskServer = CrossTestKtServerAllEntities
    taskClient = getCsTaskByName("CrossTestCsClientAllEntities")

    addDependencies()
}

task CrossTestKtCsBigBuffer(type: InteropTask) {
    taskServer = CrossTestKtServerBigBuffer
    taskClient = getCsTaskByName("CrossTestCsClientBigBuffer")

    addDependencies()
}

task CrossTestKtCsRdCall(type: InteropTask) {
    taskServer = CrossTestKtServerRdCall
    taskClient = getCsTaskByName("CrossTestCsClientRdCall")

    addDependencies()
}*/
//endregion

//val interopTasks = listOf(/*CrossTestKtCppAllEntities, CrossTestKtCppBigBuffer, CrossTestKtCppRdCall,*/
//                    CrossTestKtCsAllEntities, CrossTestKtCsBigBuffer, CrossTestKtCsRdCall)

//task crossTest() {
//    dependsOn interopTasks
//}

/*interopTasks.each { t ->
    task "${t.name}Run"(type: Test) {
        dependsOn t

        useJUnit()
        testNameIncludePatterns = ["*${t.name}*".toString()]
    }

    crossTest.dependsOn "${t.name}Run"
}*/


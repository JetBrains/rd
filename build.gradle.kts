import com.jetbrains.rd.gradle.dependencies.kotlinVersion
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

buildscript {
    project.extra.apply {
        val repoRoot = rootProject.projectDir
        set("repoRoot", repoRoot)
        set("cppRoot", File(repoRoot, "rd-cpp"))
        set("ktRoot", File(repoRoot, "rd-kt"))
        set("csRoot", File(repoRoot, "rd-net"))
    }
}

plugins {
    base
    id("me.filippov.gradle.jvm.wrapper") version "0.10.0"
}

allprojects {
    plugins.apply("maven-publish")

    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
            force("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
            force("org.jetbrains.kotlin:kotlin-runtime:$kotlinVersion")
            force("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
        }
    }

    repositories {
        mavenCentral()
    }

    tasks {
        withType<Test> {
            testLogging {
                showStandardStreams = true
                exceptionFormat = TestExceptionFormat.FULL
            }
        }
    }
}

val clean by tasks.getting(Delete::class) {
    delete(rootProject.buildDir)
}

if (System.getenv("TEAMCITY_VERSION") == null) {
    version = "SNAPSHOT"
}

tasks {
    val nuGetTargetDir = buildDir.resolve("artifacts").resolve("nuget")
    val publishingGroup = "publishing"

    val dotNetBuild by registering(Exec::class) {
        group = publishingGroup
        executable = projectDir.resolve("rd-net").resolve("dotnet.cmd").canonicalPath
        args("build", "/p:Configuration=Release", "/p:PackageVersion=$version", projectDir.resolve("rd-net").resolve("Rd.sln").canonicalPath)
        environment("DOTNET_NOLOGO", "1")
        environment("DOTNET_CLI_TELEMETRY_OPTOUT", "1")
    }

    val packNuGetLifetimes by registering(Exec::class) {
        group = publishingGroup
        dependsOn(dotNetBuild)
        executable = project.projectDir.resolve("rd-net").resolve("dotnet.cmd").canonicalPath
        args("pack", "--include-symbols", "/p:Configuration=Release", "/p:PackageVersion=$version", projectDir.resolve("rd-net").resolve("Lifetimes").resolve("Lifetimes.csproj").canonicalPath)
        environment("DOTNET_NOLOGO", "1")
        environment("DOTNET_CLI_TELEMETRY_OPTOUT", "1")
    }

    val copyNuGetLifetimes by registering(Copy::class) {
        group = publishingGroup
        dependsOn(packNuGetLifetimes)
        from("${projectDir.resolve("rd-net").resolve("Lifetimes").resolve("bin").resolve("Release").canonicalPath}${File.separator}")
        include("*.nupkg")
        include("*.snupkg")
        into(buildDir.resolve("artifacts").resolve("nuget"))
    }

    val packDotNetRdFramework by registering(Exec::class) {
        group = publishingGroup
        dependsOn(dotNetBuild)
        executable = project.projectDir.resolve("rd-net").resolve("dotnet.cmd").canonicalPath
        args("pack", "--include-symbols", "/p:Configuration=Release", "/p:PackageVersion=$version", projectDir.resolve("rd-net").resolve("RdFramework").resolve("RdFramework.csproj").canonicalPath)
        environment("DOTNET_NOLOGO", "1")
        environment("DOTNET_CLI_TELEMETRY_OPTOUT", "1")
    }

    val copyNuGetRdFramework by registering(Copy::class) {
        group = publishingGroup
        dependsOn(packDotNetRdFramework)
        from("${projectDir.resolve("rd-net").resolve("RdFramework").resolve("bin").resolve("Release").canonicalPath}${File.separator}")
        include("*.nupkg")
        include("*.snupkg")
        into(buildDir.resolve("artifacts").resolve("nuget"))
    }

    val packDotNetRdFrameworkReflection by registering(Exec::class) {
        group = publishingGroup
        dependsOn(dotNetBuild)
        executable = project.projectDir.resolve("rd-net").resolve("dotnet.cmd").canonicalPath
        args("pack", "--include-symbols", "/p:Configuration=Release", "/p:PackageVersion=$version", projectDir.resolve("rd-net").resolve("RdFramework.Reflection").resolve("RdFramework.Reflection.csproj").canonicalPath)
        environment("DOTNET_NOLOGO", "1")
        environment("DOTNET_CLI_TELEMETRY_OPTOUT", "1")
    }

    val copyNuGetRdFrameworkReflection by registering(Copy::class) {
        group = publishingGroup
        dependsOn(packDotNetRdFrameworkReflection)
        from("${projectDir.resolve("rd-net").resolve("RdFramework.Reflection").resolve("bin").resolve("Release").canonicalPath}${File.separator}")
        include("*.nupkg")
        include("*.snupkg")
        into(buildDir.resolve("artifacts").resolve("nuget"))
    }

    val layoutRdGen by registering {
        group = publishingGroup
        doLast {
            val libFolder = buildDir.resolve("temp").resolve("lib").resolve("net")
            libFolder.mkdirs()
            val stubFile = libFolder.resolve("_._")
            if (!stubFile.exists()) {
                stubFile.createNewFile()
            }
        }
    }

    val packRdGen by registering(Exec::class) {
        group = publishingGroup
        dependsOn(":rd-gen:fatJar")
        dependsOn(dotNetBuild, layoutRdGen, ":rd-gen:fatJar")
        executable = project.projectDir.resolve("rd-net").resolve("dotnet.cmd").canonicalPath
        args("pack", "--output", buildDir.resolve("temp").canonicalPath, "/p:Configuration=Release", "/p:PackageVersion=$version", projectDir.resolve("rd-kt").resolve("rd-gen").resolve("JetBrains.RdGen.proj").canonicalPath)
        workingDir = projectDir.resolve("rd-kt").resolve("rd-gen")
        environment("DOTNET_NOLOGO", "1")
        environment("DOTNET_CLI_TELEMETRY_OPTOUT", "1")
    }

    val copyRdGen by registering(Copy::class) {
        group = publishingGroup
        dependsOn(packRdGen)
        from(buildDir.resolve("temp").canonicalPath)
        include("*.nupkg")
        into(buildDir.resolve("artifacts").resolve("nuget"))
    }

    val cleanupArtifacts by registering {
        group = publishingGroup
        doLast {
            if (nuGetTargetDir.exists()) {
                nuGetTargetDir.deleteRecursively()
            }
        }
    }

    val createNuGetPackages by registering {
        group = publishingGroup
        dependsOn(cleanupArtifacts, copyNuGetLifetimes, copyNuGetRdFramework, copyNuGetRdFrameworkReflection, copyRdGen)
    }

    fun enableNuGetPublishing(url: String, apiKey: String) {
        val args = mutableListOf<Any>(
            project.projectDir.resolve("rd-net").resolve("dotnet.cmd").canonicalPath,
            "nuget",
            "push",
            "--source", url,
            "--api-key", apiKey
        )

        for (file in nuGetTargetDir.listFiles().filter { it.extension == "nupkg" }) {
            exec {
                val argsForCurrentFile = (args + file).toTypedArray()
                commandLine(*argsForCurrentFile)
            }
        }
    }

    val publishNuGet by registering {
        group = publishingGroup
        dependsOn(createNuGetPackages)

        val deployToNuGetOrg = rootProject.extra["deployNuGetToNuGetOrg"].toString().toBoolean()
        val deployToInternal = rootProject.extra["deployNuGetToInternal"].toString().toBoolean()

        doLast {
            if (deployToNuGetOrg) {
                val nuGetOrgApiKey = rootProject.extra["nuGetOrgApiKey"].toString()
                enableNuGetPublishing("https://api.nuget.org/v3/index.json", nuGetOrgApiKey)
            }
            if (deployToInternal) {
                val internalFeedUrl = rootProject.extra["internalNuGetFeedUrl"].toString()
                val internalFeedApiKey = rootProject.extra["internalNuGetFeedKey"].toString()
                enableNuGetPublishing(internalFeedUrl, internalFeedApiKey)
            }
        }
    }

    named("publish") {
        dependsOn(publishNuGet)
    }
}

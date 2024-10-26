import com.jetbrains.rd.gradle.dependencies.kotlinVersion
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

buildscript {
    dependencies {
        classpath("com.squareup.okhttp3:okhttp:4.12.0")
    }
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
    id("me.filippov.gradle.jvm.wrapper") version "0.14.0"
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
        withType<KotlinCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
        withType<JavaCompile> {
            targetCompatibility = "17"
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
        dependsOn(cleanupArtifacts, copyNuGetLifetimes, copyNuGetRdFramework, copyNuGetRdFrameworkReflection)
    }

    fun enableNuGetPublishing(url: String, apiKey: String) {
        val args = mutableListOf<Any>(
            project.projectDir.resolve("rd-net").resolve("dotnet.cmd").canonicalPath,
            "nuget",
            "push",
            "--source", url,
            "--api-key", apiKey
        )

        for (file in nuGetTargetDir.listFiles()?.filter { it.extension == "nupkg" } ?: emptyList()) {
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
                val internalFeedApiKey = rootProject.extra["internalDeployKey"].toString()
                enableNuGetPublishing(internalFeedUrl, internalFeedApiKey)
            }
        }
    }

    val packSonatypeCentralBundle by registering(Zip::class) {
        group = publishingGroup

        dependsOn(
            ":rd-core:publishAllPublicationsToArtifactsRepository",
            ":rd-framework:publishAllPublicationsToArtifactsRepository",
            ":rd-gen:publishAllPublicationsToArtifactsRepository",
            ":rd-swing:publishAllPublicationsToArtifactsRepository",
            ":rd-text:publishAllPublicationsToArtifactsRepository"
        )

        from(layout.buildDirectory.dir("artifacts/maven"))
        archiveFileName.set("bundle.zip")
        destinationDirectory.set(layout.buildDirectory)
    }

    val publishMavenToCentralPortal by registering {
        group = publishingGroup

        dependsOn(packSonatypeCentralBundle)

        doLast {
            // https://central.sonatype.org/publish/publish-portal-api/#uploading-a-deployment-bundle
            val uriBase = rootProject.extra["centralPortalApiUrl"] as String
            val publicationType = "AUTOMATIC"
            val deploymentName = "rd-$version"
            val uri = "$uriBase?name=$deploymentName&publicationType=$publicationType"

            val userName = rootProject.extra["centralPortalUserName"] as String
            val token = rootProject.extra["centralPortalToken"] as String
            val base64Auth = Base64.getEncoder().encode("$userName:$token".toByteArray()).toString(Charsets.UTF_8)
            val bundleFile = packSonatypeCentralBundle.get().archiveFile.get().asFile

            println("Sending request to $uri...")

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(uri)
                .header("Authorization", "Bearer $base64Auth")
                .post(
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("bundle", bundleFile.name, bundleFile.asRequestBody())
                        .build()
                )
                .build()
            val response = client.newCall(request).execute()

            val statusCode = response.code
            println("Upload status code: $statusCode")
            println("Upload result: ${response.body!!.string()}")
            if (statusCode != 201) {
                error("Upload error to Central repository. Status code $statusCode.")
            }
        }
    }

    named("publish") {
        dependsOn(publishNuGet)

        val deployToCentral = rootProject.extra["deployMavenToCentralPortal"].toString().toBoolean()
        if (deployToCentral) {
            dependsOn(publishMavenToCentralPortal)
        }
    }
}

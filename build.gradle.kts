import com.jetbrains.rd.gradle.dependencies.kotlinVersion
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

buildscript {
    dependencies {
        classpath("com.squareup.okhttp3:okhttp:5.3.0")
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

    fun base64Auth(userName: String, accessToken: String): String =
        Base64.getEncoder().encode("$userName:$accessToken".toByteArray()).toString(Charsets.UTF_8)

    fun deployToCentralPortal(
        bundleFile: File,
        uriBase: String,
        isUserManaged: Boolean,
        deploymentName: String,
        userName: String,
        accessToken: String
    ): String {
        // https://central.sonatype.org/publish/publish-portal-api/#uploading-a-deployment-bundle
        val publishingType = if (isUserManaged) "USER_MANAGED" else "AUTOMATIC"
        val uri = uriBase.trimEnd('/') + "/api/v1/publisher/upload?name=$deploymentName&publishingType=$publishingType"
        val base64Auth = base64Auth(userName, accessToken)

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
        val uploadResult = response.body!!.string()
        println("Upload result: $uploadResult")
        if (statusCode == 201) {
            return uploadResult
        } else {
            error("Upload error to Central repository. Status code $statusCode.")
        }
    }

    fun waitForUploadToSucceed(
        uriBase: String,
        deploymentId: String,
        isUserManaged: Boolean,
        userName: String,
        accessToken: String,
        maxTimeout: Duration,
        minTimeBetweenAttempts: Duration
    ) {
        val uri = uriBase.trimEnd('/') + "/api/v1/publisher/status?id=$deploymentId"
        val base64Auth = base64Auth(userName, accessToken)

        var timeSpent = Duration.ZERO
        var attemptNumber = 1
        var terminatingState = false

        println("Polling for deployment status for $maxTimeout: $uri")

        while (timeSpent < maxTimeout) {
            val remainingTime = maxTimeout - timeSpent
            println("Polling attempt ${attemptNumber++}, remaining time ${remainingTime}.")

            val client = OkHttpClient().newBuilder()
                .callTimeout(remainingTime.toJavaDuration())
                .build()

            val beforeMs = System.currentTimeMillis()
            try {
                val request = Request.Builder()
                    .url(uri)
                    .header("Authorization", "Bearer $base64Auth")
                    .post("".toRequestBody())
                    .build()
                val response = client.newCall(request).execute()
                val code = response.code
                if (code != 200) {
                    error("Response code $code: ${response.body?.string()}")
                }

                val jsonResult = JsonSlurper().parse(response.body?.bytes() ?: error("Empty response body.")) as Map<*, *>
                val state = jsonResult["deploymentState"]
                println("Current state: $state.")

                when(state) {
                    "PENDING", "VALIDATING", "PUBLISHING" -> {}
                    "VALIDATED" -> {
                        terminatingState = true

                        if (isUserManaged) {
                            println("Validated successfully.")
                            return
                        }

                        error("State error: deployment is not user managed, but signals it requires a UI interaction.")
                    }
                    "PUBLISHED" -> {
                        terminatingState = true

                        if (!isUserManaged) {
                            println("Published successfully.")
                            return
                        }

                        error("State error: deployment is user managed, but signals it has been published.")
                    }
                    "FAILED" -> {
                        terminatingState = true

                        // The documentation provides no type information for the errors field, so we have to treat
                        // them as opaque.
                        val errors = jsonResult["errors"]
                        val errorsAsString = JsonBuilder(errors).toPrettyString()
                        error("Deployment failed. Errors: $errorsAsString")
                    }
                    else -> logger.warn("Unknown deployment state: $state")
                }
            } catch (e: Exception) {
                if (terminatingState) {
                    throw e
                }

                logger.warn("Error during HTTP request: ${e.message}")
            } finally {
                val afterMs = System.currentTimeMillis()
                var attemptTime = (afterMs - beforeMs).coerceAtLeast(0L).milliseconds
                if (attemptTime < minTimeBetweenAttempts) {
                   val sleepTime = minTimeBetweenAttempts - attemptTime
                   Thread.sleep(sleepTime.inWholeMilliseconds)
                   attemptTime = minTimeBetweenAttempts
                }

                timeSpent += attemptTime
            }
        }
    }

    val publishMavenToCentralPortal by registering {
        group = publishingGroup

        dependsOn(packSonatypeCentralBundle)

        doLast {
            val uriBase = rootProject.extra["centralPortalUrl"] as String
            val userName = rootProject.extra["centralPortalUserName"] as String
            val accessToken = rootProject.extra["centralPortalToken"] as String
            val isUserManaged = false

            val deploymentId = deployToCentralPortal(
                bundleFile = packSonatypeCentralBundle.get().archiveFile.get().asFile,
                uriBase,
                isUserManaged,
                deploymentName = "rd-$version",
                userName,
                accessToken
            )
            waitForUploadToSucceed(
                uriBase,
                deploymentId,
                isUserManaged,
                userName,
                accessToken,
                maxTimeout = 60.minutes,
                minTimeBetweenAttempts = 5.seconds
            )
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

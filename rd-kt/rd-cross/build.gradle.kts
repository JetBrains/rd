import com.jetbrains.rd.gradle.plugins.applyKotlinJVM

applyKotlinJVM()

apply(from = "cross.gradle.kts")

/*
tasks {
    val test by creating(DefaultTask::class) {
        dependsOn("crossTest")
    }
}*/

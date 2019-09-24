import com.jetbrains.rd.gradle.plugins.applyKotlinJVM

applyKotlinJVM()
//apply from: "models.gradle"
apply(from = "cross.gradle.kts")


/*
task test(type: DefaultTask, overwrite: true) {
    dependsOn "crossTest"
}*/

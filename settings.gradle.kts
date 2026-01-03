rootProject.name = "rd"

//enableFeaturePreview("GRADLE_METADATA")

// Application agnostic common code (aka utils, shared, common, misc)

include(":rd-core")
project(":rd-core").projectDir = File("rd-kt/rd-core")

include(":rd-framework")
project(":rd-framework").projectDir = File("rd-kt/rd-framework")

include(":rd-gen")
project(":rd-gen").projectDir = File("rd-kt/rd-gen")
include(":rd-gen:models")
project(":rd-gen:models").projectDir = File("rd-kt/rd-gen/models")

include(":rd-text")
project(":rd-text").projectDir = File("rd-kt/rd-text")

include(":rd-swing")
project(":rd-swing").projectDir = File("rd-kt/rd-swing")

val TEAMCITY_VERSION = "TEAMCITY_VERSION"

if (System.getenv(TEAMCITY_VERSION) == null) {
    include(":rd-net")
    project(":rd-net").projectDir = File("rd-net")

//    include(":rd-cpp")
//    project(":rd-cpp").projectDir = File("rd-cpp")

//    include(":rd-cross")
//    project(":rd-cross").projectDir = File("rd-kt/rd-cross")
}

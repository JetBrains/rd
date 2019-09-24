rootProject.name = "rd"

enableFeaturePreview("GRADLE_METADATA")

// Application agnostic common code (aka utils, shared, common, misc)

include(":rd-core")
project(":rd-core").projectDir =  File("rd-kt/rd-core")

include(":rd-framework")
project(":rd-framework").projectDir =  File("rd-kt/rd-framework")

include(":rd-gen")
project(":rd-gen").projectDir =  File("rd-kt/rd-gen")

include(":rd-text")
project(":rd-text").projectDir =  File("rd-kt/rd-text")

include(":rd-swing")
project(":rd-swing").projectDir =  File("rd-kt/rd-swing")

//include ":rd-cpp"

//include ":rd-net"

//include ":rd-cross"


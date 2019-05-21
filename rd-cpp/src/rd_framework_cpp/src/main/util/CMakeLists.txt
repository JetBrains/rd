add_library(rd_framework_cpp_util STATIC
        hashing.h hashing.cpp
        framework_traits.h guards.h)

target_include_directories(rd_framework_cpp_util PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
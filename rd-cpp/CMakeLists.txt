cmake_minimum_required(VERSION 3.7)

project(
        rd_cpp
        VERSION 1.0
#        DESCRIPTION "C++ implementation of JetBrains IPC"
#        HOMEPAGE_URL "https://github.com/jetbrains/rd"
        LANGUAGES CXX
)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(CMAKE_INSTALL_LIBS "${CMAKE_CURRENT_LIST_DIR}/export/libs")
set(CMAKE_INSTALL_PUBLIC_HEADER "${CMAKE_CURRENT_LIST_DIR}/export/include")
set(CMAKE_INSTALL_PUBLIC_HEADER_THIRDPARTY "${CMAKE_INSTALL_PUBLIC_HEADER}/thirdparty")

if (CMAKE_COMPILER_IS_GNUCC OR CMAKE_COMPILER_IS_GNUCXX OR CMAKE_CXX_COMPILER_ID MATCHES "Clang")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -pedantic")
    set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -D_GLIBCXX_DEBUG")
    set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O2")
    if (CMAKE_CXX_COMPILER_ID MATCHES "Clang")
        if (CMAKE_CXX_COMPILER_VERSION VERSION_LESS "6.0.0")
            message(FATAL_ERROR "Insufficient clang version")
        endif ()
    endif ()
    if (MINGW)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wa,-mbig-obj")
    endif ()
elseif (MSVC)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
endif ()

set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)

option(ENABLE_TESTS_OPTION "Global enable test" ON)
option(INSTALL_GTEST OFF)

set(RD_CORE_CPP_ENABLE_TESTS  ${ENABLE_TESTS_OPTION} CACHE BOOL "Enable core tests." FORCE)
set(RD_FRAMEWORK_CPP_ENABLE_TESTS  ${ENABLE_TESTS_OPTION} CACHE BOOL "Enable framework tests." FORCE)
set(RD_GEN_CPP_ENABLE_TESTS  ${ENABLE_TESTS_OPTION} CACHE BOOL "Enable gen tests." FORCE)

option(ENABLE_PCH_HEADERS "Enable precompiled headers" ON)
if (ENABLE_PCH_HEADERS)
    set(PCH_CPP_OPT pch.cpp)
else ()
    set(PCH_CPP_OPT "")
endif ()

add_subdirectory(thirdparty)
add_subdirectory(src)

if (ENABLE_TESTS_OPTION)
    if(NOT EXISTS "${PROJECT_SOURCE_DIR}/demo")
        message(FATAL_ERROR "Run 'gradlew generateEverything' in rd root to generate missing folders.")
    endif()
    add_subdirectory(googletest)
    add_subdirectory(demo)
endif ()

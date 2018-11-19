cmake_minimum_required(VERSION 3.10)

project(rd_cpp)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(CMAKE_COMPILER_IS_GNUCC OR CMAKE_COMPILER_IS_GNUCXX)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -pedantic")
    set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -D_GLIBCXX_DEBUG")
elseif(MSVC)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
endif()

set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)

set(ENABLE_TESTS_OPTION false)

SET(ENABLE_TESTS $<IF:ENABLE_TESTS_OPTION,ON,OFF>)
option(OPTIONAL_ENABLE_TESTS "Enable tests." OFF)
option(OPTIONAL_ENABLE_DOCS "Enable documentation." OFF)
option(RD_CORE_CPP_ENABLE_TESTS "Enable tests." ${ENABLE_TESTS})
option(RD_FRAMEWORK_CPP_ENABLE_TESTS "Enable tests." ${ENABLE_TESTS})

if(ENABLE_TESTS_OPTION)
    add_subdirectory(googletest)
endif()
add_subdirectory(optional)
add_subdirectory(variant)
add_subdirectory(rd_core_cpp)
add_subdirectory(rd_framework_cpp)
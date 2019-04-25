cmake_minimum_required(VERSION 3.10.2)

project(rd_cpp)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(CMAKE_INSTALL_LIBS "${CMAKE_CURRENT_LIST_DIR}/export/libs")
set(CMAKE_INSTALL_PUBLIC_HEADER "${CMAKE_CURRENT_LIST_DIR}/export/include")

if(CMAKE_COMPILER_IS_GNUCC OR CMAKE_COMPILER_IS_GNUCXX)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} --std=c++14 -Wall -pedantic")
    set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -D_GLIBCXX_DEBUG")
elseif(MSVC)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
elseif (CMAKE_CXX_COMPILER_ID STREQUAL "Clang")
    if(CMAKE_CXX_COMPILER_VERSION VERSION_LESS "6.0.0")
        message(FATAL_ERROR "Insufficient clang version")
    endif()
endif()

set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)

option(ENABLE_TESTS_OPTION "Global enable test" ON)

option(RD_CORE_CPP_ENABLE_TESTS "Enable tests." ${ENABLE_TESTS_OPTION})
option(RD_FRAMEWORK_CPP_ENABLE_TESTS "Enable tests." ${ENABLE_TESTS_OPTION})

if(ENABLE_TESTS_OPTION)
    add_subdirectory(googletest)
endif()
add_subdirectory(thirdparty)
add_subdirectory(src)

if(ENABLE_TESTS_OPTION)
    add_subdirectory(cpp_model)
    add_subdirectory(demo)
endif()
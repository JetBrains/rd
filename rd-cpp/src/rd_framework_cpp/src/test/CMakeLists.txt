#exec

add_executable(rd_framework_cpp_test
        #pch
        ${PCH_CPP_OPT}
        #cases
        cases/BufferTest.cpp
        cases/RdIdTest.cpp
        cases/RdPropertyTest.cpp
        cases/RdSignalTest.cpp
        cases/RdListTest.cpp
        cases/RdSetTest.cpp
        cases/RdMapTest.cpp
        cases/SocketWireTest.cpp
        cases/RdExtTest.cpp
        cases/RdTaskTest.cpp
        cases/DynamicPolymorphicTest.cpp
        cases/InterningTest.cpp
        cases/BackgroundSchedulerTest.cpp
        cases/SocketProxyTest.cpp
        cases/RdAsyncTaskTest.cpp
        cases/RdAsyncSignalTest.cpp)

message(STATUS "Using pch by rd_framework_test: '${ENABLE_PCH_HEADERS}'")

if (ENABLE_PCH_HEADERS)
    include(${PROJECT_SOURCE_DIR}/PrecompiledHeader.cmake)
    add_precompiled_header(rd_framework_cpp_test pch.h SOURCE_CXX pch.cpp FORCEINCLUDE)
endif ()

add_subdirectory(util)
add_subdirectory(kotlin_interaction)

target_include_directories(rd_framework_cpp_test PUBLIC cases)
target_include_directories(rd_framework_cpp_test PUBLIC util)

target_link_libraries(rd_framework_cpp_test
        gtest gtest_main rd_framework_cpp rd_framework_cpp_test_util rd_core_cpp_test_util)

get_target_property(rd_core_cpp_type rd_core_cpp TYPE)
if(rd_core_cpp_type STREQUAL "SHARED_LIBRARY")
    add_custom_command(TARGET rd_framework_cpp_test POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
            $<TARGET_FILE:rd_core_cpp> $<TARGET_FILE_DIR:rd_framework_cpp_test>
            COMMENT "Copying rd_core_cpp shared lib alongside tests."
            VERBATIM)
endif()

get_target_property(rd_framework_cpp_type rd_framework_cpp TYPE)
if(rd_framework_cpp_type STREQUAL "SHARED_LIBRARY")
    add_custom_command(TARGET rd_framework_cpp_test POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
            $<TARGET_FILE:rd_framework_cpp> $<TARGET_FILE_DIR:rd_framework_cpp_test>
            COMMENT "Copying rd_framework_cpp shared lib alongside tests."
            VERBATIM)
endif()
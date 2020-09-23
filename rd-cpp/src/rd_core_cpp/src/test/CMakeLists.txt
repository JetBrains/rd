#exec

add_executable(rd_core_cpp_test
        cases/SignalTest.cpp
        cases/ViewableMapTest.cpp
        cases/PropertyTest.cpp
        cases/ViewableSetTest.cpp
        cases/AdviseVsViewTest.cpp
        cases/ViewableListTest.cpp cases/GeneratorUtilTest.cpp
        #pch
        ${PCH_CPP_OPT}
        )

message(STATUS "Using pch by rd_core_test: '${ENABLE_PCH_HEADERS}'")

if (ENABLE_PCH_HEADERS)
    include(${PROJECT_SOURCE_DIR}/PrecompiledHeader.cmake)
    add_precompiled_header(rd_core_cpp_test pch.h SOURCE_CXX pch.cpp FORCEINCLUDE)
endif ()

add_subdirectory(util)

target_include_directories(rd_core_cpp_test PUBLIC cases)
target_include_directories(rd_core_cpp_test PUBLIC util)

target_link_libraries(rd_core_cpp_test gtest gtest_main rd_core_cpp rd_core_cpp_test_util)

get_target_property(rd_core_cpp_type rd_core_cpp TYPE)
if(rd_core_cpp_type STREQUAL "SHARED_LIBRARY")
    add_custom_command(TARGET rd_core_cpp_test POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
            $<TARGET_FILE:rd_core_cpp> $<TARGET_FILE_DIR:rd_core_cpp_test>
            COMMENT "Copying rd_core_cpp shared lib alongside tests."
            VERBATIM)
endif()
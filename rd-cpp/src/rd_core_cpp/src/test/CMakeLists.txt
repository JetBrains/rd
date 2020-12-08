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

copy_shared_dependency_if_needed(rd_core_cpp_test rd_core_cpp spdlog)
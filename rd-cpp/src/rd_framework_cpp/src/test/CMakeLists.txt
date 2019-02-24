#exec

add_executable(rd_framework_cpp_test
        util/RdFrameworkTestBase.cpp util/RdFrameworkTestBase.h
        util/SocketWireTestBase.cpp util/SocketWireTestBase.h
		util/RdFrameworkDynamicPolymorphicTestBase.cpp util/RdFrameworkDynamicPolymorphicTestBase.h

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
        cases/DynamicPolymorphicTest.cpp)

add_subdirectory(util)
add_subdirectory(kotlin_interaction)

target_include_directories(rd_framework_cpp_test PUBLIC cases)
target_include_directories(rd_framework_cpp_test PUBLIC util)

target_link_libraries(rd_framework_cpp_test gtest gtest_main rd_framework_cpp rd_framework_cpp_util rd_core_cpp_test_util)


#exec

add_executable(rd_framework_cpp_test
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
#        cases/InterningTest.cpp
        )

add_subdirectory(util)
add_subdirectory(kotlin_interaction)

target_include_directories(rd_framework_cpp_test PUBLIC cases)
target_include_directories(rd_framework_cpp_test PUBLIC util)

target_link_libraries(rd_framework_cpp_test gtest gtest_main rd_framework_cpp rd_framework_cpp_test_util rd_core_cpp_test_util)


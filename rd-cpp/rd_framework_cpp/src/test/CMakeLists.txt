#exec

add_executable(rd_framework_cpp_test
        cases/RdIdTest.cpp
        cases/RdPropertyTest.cpp
        util/RdFrameworkTestBase.cpp util/RdFrameworkTestBase.h
        util/TestWire.cpp
        util/TestWire.h
        cases/RdSignalTest.cpp
        cases/RdListTest.cpp
        cases/RdSetTest.cpp
        cases/RdMapTest.cpp
        util/DynamicEntity.cpp util/DynamicEntity.h
        cases/SocketWireTest.cpp
        util/SocketWireTestBase.cpp util/SocketWireTestBase.h
        util/TestScheduler.cpp util/TestScheduler.h
        util/WireUtil.cpp util/WireUtil.h
        cases/RdExtTest.cpp
        util/DynamicExt.cpp util/DynamicExt.h
        util/ExtProperty.cpp
        util/ExtProperty.h
        cases/BufferTest.cpp
        cases/RdTaskTest.cpp
        )

target_include_directories(rd_framework_cpp_test PUBLIC cases)
target_include_directories(rd_framework_cpp_test PUBLIC util)

target_link_libraries(rd_framework_cpp_test gtest gtest_main rd_framework_cpp)

if (UNIX)
    add_definitions(-D_LINUX)
endif ()
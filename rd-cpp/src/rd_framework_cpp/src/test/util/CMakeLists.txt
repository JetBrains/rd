add_library(rd_framework_cpp_test_util
        #entity
        entity/AbstractEntity.cpp
        entity/AbstractEntity.h
        entity/ConcreteEntity.cpp
        entity/ConcreteEntity.h
        entity/ConcreteEntity_Unknown.cpp
        entity/ConcreteEntity_Unknown.h
        entity/DynamicEntity.cpp
        entity/DynamicEntity.h
        entity/DynamicExt.cpp
        entity/DynamicExt.h
        entity/ExtProperty.h
        entity/FakeEntity.cpp
        entity/FakeEntity.h
        #test_fixture
        test_fixture/SocketWireTestBase.cpp test_fixture/SocketWireTestBase.h
        test_fixture/RdFrameworkTestBase.cpp test_fixture/RdFrameworkTestBase.h
        test_fixture/RdFrameworkDynamicPolymorphicTestBase.cpp test_fixture/RdFrameworkDynamicPolymorphicTestBase.h
        #        test_fixture/InterningTestBase.cpp test_fixture/InterningTestBase.h

        SimpleScheduler.cpp SimpleScheduler.h
        WireUtil.cpp WireUtil.h
        SimpleWire.cpp SimpleWire.h)

#add_subdirectory(models)

target_include_directories(rd_framework_cpp_test_util PUBLIC ${CMAKE_CURRENT_SOURCE_DIR} entity models test_fixture)

target_link_libraries(rd_framework_cpp_test_util PUBLIC
        gtest gtest_main
        rd_framework_cpp
        rd_core_cpp_test_util)
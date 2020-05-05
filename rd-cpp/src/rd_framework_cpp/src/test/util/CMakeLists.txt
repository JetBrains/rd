add_library(rd_framework_cpp_test_util
        #others
        others/entities_util.cpp
        #scheduler
        scheduler/TestSingleThreadScheduler.cpp
        #test_fixture
        test_fixture/SocketWireTestBase.cpp
        test_fixture/RdFrameworkTestBase.cpp
        test_fixture/RdFrameworkDynamicPolymorphicTestBase.cpp
        test_fixture/InterningTestBase.cpp
        test_fixture/RdAsyncTestBase.cpp
        #wire
        wire/SocketProxy.cpp

        Linearization.cpp
        PropertyHolderWithInternRoot.cpp
        SimpleWire.cpp
        )

add_subdirectory(entities)
add_subdirectory(interning)

target_include_directories(rd_framework_cpp_test_util PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}
        entities
        models
        test_fixture
        others
        scheduler)

target_link_libraries(rd_framework_cpp_test_util PUBLIC
        gtest gtest_main
        rd_framework_cpp
        rd_core_cpp_test_util
        entities
        interning_test_model)
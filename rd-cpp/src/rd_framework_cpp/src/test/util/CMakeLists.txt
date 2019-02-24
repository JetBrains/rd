add_library(rd_framework_cpp_util
        ExtProperty.h
        DynamicExt.cpp DynamicExt.h
        SimpleScheduler.cpp SimpleScheduler.h
        WireUtil.cpp WireUtil.h
        DynamicEntity.cpp DynamicEntity.h
        AbstractEntity.cpp AbstractEntity.h
        ConcreteEntity.cpp ConcreteEntity.h
        ConcreteEntity_Unknown.cpp ConcreteEntity_Unknown.h
        SimpleWire.cpp SimpleWire.h
        FakeEntity.cpp FakeEntity.h)

target_include_directories(rd_framework_cpp_util PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

target_link_libraries(rd_framework_cpp_util PUBLIC rd_framework_cpp)
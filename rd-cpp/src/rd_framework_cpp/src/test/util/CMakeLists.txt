add_library(rd_framework_cpp_util
        ExtProperty.cpp ExtProperty.h
        DynamicExt.cpp DynamicExt.h
        TestScheduler.cpp TestScheduler.h
        WireUtil.cpp WireUtil.h
        DynamicEntity.cpp DynamicEntity.h
        TestWire.cpp TestWire.h
        )

target_include_directories(rd_framework_cpp_util PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

target_link_libraries(rd_framework_cpp_util PUBLIC rd_framework_cpp)
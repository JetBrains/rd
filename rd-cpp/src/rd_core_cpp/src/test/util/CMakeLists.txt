add_library(rd_core_cpp_test_util STATIC
        filesystem.cpp filesystem.h)

target_link_libraries(rd_core_cpp_test_util rd_core_cpp)

target_include_directories(rd_core_cpp_test_util PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})


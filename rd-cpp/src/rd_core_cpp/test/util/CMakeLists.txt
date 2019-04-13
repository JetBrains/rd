add_library(rd_core_cpp_test_util STATIC
        test_util.cpp test_util.h)

target_link_libraries(rd_core_cpp_test_util rd_core_cpp)

target_include_directories(rd_core_cpp_test_util PUBLIC .)


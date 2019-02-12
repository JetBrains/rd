add_subdirectory(src/main)
if (RD_CORE_CPP_ENABLE_TESTS)
    add_subdirectory(src/test)
endif ()

#target_link_libraries(rd_core_cpp_test gtest gtest_main)
#target_link_libraries(rd_core_cpp_test rd_core_cpp)
add_library(rd_core_util PUBLIC core_util.h erase_if.h gen_util.h overloaded.h wrapper.h)

target_include_directories(rd_core_util PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
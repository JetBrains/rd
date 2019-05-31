add_library(rd_core_cpp_util STATIC
        Void.cpp Void.h
        core_util.h erase_if.h gen_util.h overloaded.h wrapper.h shared_function.h
        core_traits.h
        to_string.cpp to_string.h
        Logger.cpp Logger.h wrapper.cpp
        filesystem.cpp filesystem.h
        DateTime.cpp DateTime.h)

target_include_directories(rd_core_cpp_util PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(rd_core_cpp_util rd_core_cpp)
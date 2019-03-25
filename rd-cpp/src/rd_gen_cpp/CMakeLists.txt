add_library(rd_gen_cpp STATIC
        RdTextBuffer.cpp RdTextBuffer.h
        )

target_include_directories(rd_gen_cpp PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
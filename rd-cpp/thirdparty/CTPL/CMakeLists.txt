add_library(ctpl INTERFACE)
target_sources(ctpl INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/include/ctpl_stl.h)
target_include_directories(ctpl INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/include)
cmake_minimum_required(VERSION 3.0)

project(optional)

add_library(optional INTERFACE)
target_sources(optional INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/tl/optional.hpp)
target_include_directories(optional INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/tl)

install(DIRECTORY tl DESTINATION ${CMAKE_INSTALL_PUBLIC_HEADER}/optional)

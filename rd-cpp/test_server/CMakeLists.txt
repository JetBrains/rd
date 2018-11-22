cmake_minimum_required(VERSION 3.10)

add_executable(test_server main.cpp UnrealEngineModel.cpp UnrealEngineModel.h)

target_link_libraries(test_server PRIVATE rd_framework_cpp rd_framework_cpp_util)
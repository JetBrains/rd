cmake_minimum_required(VERSION 3.10)

add_executable(test_server_receivable main.cpp TestModel.cpp TestModel.h)

target_link_libraries(test_server_receivable PRIVATE rd_framework_cpp rd_framework_cpp_util)
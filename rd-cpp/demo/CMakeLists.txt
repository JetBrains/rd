cmake_minimum_required(VERSION 3.10)

add_executable(demo main.cpp)
target_link_libraries(demo PRIVATE rd_framework_cpp rd_framework_cpp_util cpp_model)
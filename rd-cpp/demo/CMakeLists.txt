cmake_minimum_required(VERSION 3.10)

add_executable(demo_client main.cpp)
target_link_libraries(demo_client PRIVATE rd_framework_cpp rd_framework_cpp_util cpp_model)
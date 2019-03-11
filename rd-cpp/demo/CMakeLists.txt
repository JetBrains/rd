cmake_minimum_required(VERSION 3.10)

add_executable(demo main.cpp)

add_subdirectory(model)
target_include_directories(demo PUBLIC model)
target_link_libraries(demo PRIVATE rd_framework_cpp rd_framework_cpp_util demo_model)

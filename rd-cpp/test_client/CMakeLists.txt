cmake_minimum_required(VERSION 3.10)

add_executable(test_client main.cpp model.cpp model.hpp)
target_link_libraries(test_client PRIVATE rd_framework_cpp)
cmake_minimum_required(VERSION 3.10)

add_executable(demo main.cpp)

add_subdirectory(model)
target_include_directories(demo PUBLIC model)
target_link_libraries(demo PRIVATE rd_framework_cpp demo_model)

add_executable(profile profile.cpp)
target_include_directories(profile PUBLIC model)
target_link_libraries(profile PRIVATE rd_framework_cpp demo_model)
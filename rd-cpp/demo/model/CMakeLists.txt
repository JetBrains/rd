cmake_minimum_required(VERSION 3.10.2)
add_library(demo_model STATIC DemoModel/MyScalar.cpp
DemoModel/DemoModel.cpp
DemoModel/MyScalar.h
DemoModel/DemoModel.h
DemoRoot/DemoRoot.cpp
DemoRoot/DemoRoot.h
ExtModel/ExtModel.cpp
ExtModel/ExtModel.h)

target_include_directories(demo_model PUBLIC ${CMAKE_CURRENT_SOURCE_DIR} DemoRoot DemoModel ExtModel)
target_link_libraries(demo_model PUBLIC rd_framework_cpp)

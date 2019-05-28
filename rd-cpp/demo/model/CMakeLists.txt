cmake_minimum_required(VERSION 3.10.2)
option(ENABLE_PCH_HEADERS_FOR_demo_model "Enable precompiled headers" OFF)
if (ENABLE_PCH_HEADERS_FOR_demo_model)
    set(PCH_CPP_OPT pch.cpp)
else ()
    set(PCH_CPP_OPT "")
endif ()
add_library(demo_model STATIC DemoModel/MyEnum.cpp
DemoModel/MyScalar.cpp
DemoModel/ConstUtil.cpp
DemoModel/Base.cpp
DemoModel/Derived.cpp
DemoModel/DemoModel.cpp
DemoModel/Base_Unknown.cpp
DemoModel/MyEnum.h
DemoModel/MyScalar.h
DemoModel/ConstUtil.h
DemoModel/Base.h
DemoModel/Derived.h
DemoModel/DemoModel.h
DemoModel/Base_Unknown.h
DemoRoot/DemoRoot.cpp
DemoRoot/DemoRoot.h
ExtModel/ExtModel.cpp
ExtModel/ExtModel.h
instantiations.h
instantiations.cpp
${PCH_CPP_OPT})

target_include_directories(demo_model PUBLIC ${CMAKE_CURRENT_SOURCE_DIR} DemoRoot
DemoModel
ExtModel)
target_link_libraries(demo_model PUBLIC rd_framework_cpp)
if (ENABLE_PCH_HEADERS_FOR_demo_model)
    include(PrecompiledHeader.cmake)
    add_precompiled_header(demo_model pch.h SOURCE_CXX pch.cpp FORCEINCLUDE)
endif ()

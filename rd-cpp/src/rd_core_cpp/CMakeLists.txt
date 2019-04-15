#lib

add_library(rd_core_cpp STATIC
        #lifetime
        src/lifetime/LifetimeImpl.cpp
        src/lifetime/Lifetime.cpp
        src/lifetime/LifetimeDefinition.cpp
        src/lifetime/SequentialLifetimes.cpp
        #reactive
        ../rd_framework_cpp/src/base/IScheduler.cpp
        src/reactive/SignalX.cpp
        src/reactive/base/viewable_collections.cpp
        #pch
        ${PCH_CPP_OPT}
        )

message(STATUS "Using pch by rd_framework: '${ENABLE_PCH_HEADERS}'")

if (ENABLE_PCH_HEADERS)
    include(${PROJECT_SOURCE_DIR}/PrecompiledHeader.cmake)
    add_precompiled_header(rd_core_cpp ${PCH_H_OPT} SOURCE_CXX ${PCH_CPP_OPT} FORCEINCLUDE)
endif ()


add_subdirectory(util)

target_include_directories(rd_core_cpp
        PUBLIC
        $<INSTALL_INTERFACE:include>
        $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
        INTERFACE
        ${CMAKE_CURRENT_SOURCE_DIR}/src/
        ${CMAKE_CURRENT_SOURCE_DIR}/src/lifetime
        ${CMAKE_CURRENT_SOURCE_DIR}/src/reactive
        ${CMAKE_CURRENT_SOURCE_DIR}/src/reactive/base
#        todo include recursively
        )

target_link_libraries(rd_core_cpp PUBLIC rd_core_cpp_util thirdparty)

install(DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        DESTINATION "${CMAKE_INSTALL_PUBLIC_HEADER}/rd_core_cpp"
        CONFIGURATIONS Release
        FILES_MATCHING
        PATTERN *.h
        PATTERN *.hpp
        )

if (RD_CORE_CPP_ENABLE_TESTS)
    add_subdirectory(test)
endif ()

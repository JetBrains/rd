#lib

add_library(rd_core_cpp STATIC
        #lifetime
        lifetime/LifetimeImpl.cpp lifetime/LifetimeImpl.h
        lifetime/Lifetime.cpp lifetime/Lifetime.h
        lifetime/LifetimeDefinition.cpp lifetime/LifetimeDefinition.h
        lifetime/SequentialLifetimes.cpp lifetime/SequentialLifetimes.h
        #logger
        logger/Logger.cpp logger/Logger.h
        #reactive
        reactive/SignalX.h
        reactive/interfaces.h
        reactive/Property.h
        reactive/ViewableMap.h
        reactive/ViewableSet.h
        reactive/ViewableList.h
        reactive/base/IViewableList.h
        reactive/base/IViewableSet.h
        reactive/base/IViewableMap.h reactive/SignalX.cpp
        reactive/base/IProperty.h
        reactive/base/viewable_collections.cpp reactive/base/viewable_collections.h
        #std
        std/hash.h
        std/to_string.cpp std/to_string.h
        std/filesystem.cpp std/filesystem.h
        std/allocator.h
        std/list.h
        std/unordered_map.h
        std/unordered_set.h
        #types
        types/DateTime.cpp types/DateTime.h
        types/Void.cpp types/Void.h
        types/wrapper.cpp types/wrapper.h
        #pch
        ${PCH_CPP_OPT}
        )

message(STATUS "Using pch by rd_core: '${ENABLE_PCH_HEADERS}'")

if (ENABLE_PCH_HEADERS)
    include(${PROJECT_SOURCE_DIR}/PrecompiledHeader.cmake)
    add_precompiled_header(rd_core_cpp pch.h SOURCE_CXX pch.cpp FORCEINCLUDE)
endif ()


add_subdirectory(util)

target_include_directories(rd_core_cpp PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        )

target_link_libraries(rd_core_cpp
        PUBLIC rd_core_cpp_util thirdparty
        )

install(DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/
        DESTINATION "${CMAKE_INSTALL_PUBLIC_HEADER}/rd_core_cpp"
        CONFIGURATIONS Release
        FILES_MATCHING
        PATTERN *.h
        PATTERN *.hpp
        )

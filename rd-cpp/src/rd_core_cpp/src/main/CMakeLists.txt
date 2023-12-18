#lib
set(RD_CORE_CPP_SOURCES
        #lifetime
        lifetime/LifetimeImpl.cpp lifetime/LifetimeImpl.h
        lifetime/Lifetime.cpp lifetime/Lifetime.h
        lifetime/LifetimeDefinition.cpp lifetime/LifetimeDefinition.h
        lifetime/SequentialLifetimes.cpp lifetime/SequentialLifetimes.h
        #reactive
        reactive/base/SignalCookie.h reactive/base/SignalCookie.cpp
        reactive/base/SignalX.h
        reactive/Property.h
        reactive/ViewableMap.h
        reactive/ViewableSet.h
        reactive/ViewableList.h
        reactive/base/interfaces.h
        reactive/base/IViewableList.h
        reactive/base/IViewableSet.h
        reactive/base/IViewableMap.h
        reactive/base/IProperty.h
        reactive/base/viewable_collections.h
        #std
        std/hash.h
        std/to_string.h
        std/allocator.h
        std/list.h
        std/unordered_map.h
        std/unordered_set.h
        #types
        types/DateTime.cpp types/DateTime.h
        types/TimeSpan.cpp types/TimeSpan.h
        types/Void.h
        types/wrapper.h
        #util
        util/core_traits.h util/core_util.h
        util/enum.h util/erase_if.h
        util/gen_util.h util/overloaded.h
        #pch
        ${PCH_CPP_OPT}
)

if (RD_STATIC)
    add_library(rd_core_cpp STATIC ${RD_CORE_CPP_SOURCES})
    target_compile_definitions(rd_core_cpp PUBLIC RD_CORE_STATIC_DEFINE)
else ()
    add_library(rd_core_cpp SHARED ${RD_CORE_CPP_SOURCES})
endif ()

include(GenerateExportHeader)
generate_export_header(rd_core_cpp
        BASE_NAME rd_core
        EXPORT_MACRO_NAME RD_CORE_API)

message(STATUS "Using pch by rd_core: '${ENABLE_PCH_HEADERS}'")

if (ENABLE_PCH_HEADERS)
    include(${PROJECT_SOURCE_DIR}/PrecompiledHeader.cmake)
    add_precompiled_header(rd_core_cpp pch.h SOURCE_CXX pch.cpp FORCEINCLUDE)
endif ()

target_include_directories(rd_core_cpp PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_BINARY_DIR}
        )

target_link_libraries(rd_core_cpp
        PUBLIC thirdparty
        )

install(DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/
        DESTINATION "${CMAKE_INSTALL_PUBLIC_HEADER}/rd_core_cpp"
        CONFIGURATIONS Release
        FILES_MATCHING
        PATTERN *.h
        PATTERN *.hpp
        )

install(FILES ${CMAKE_CURRENT_BINARY_DIR}/rd_core_export.h
        DESTINATION "${CMAKE_INSTALL_PUBLIC_HEADER}/rd_core_cpp"
        )
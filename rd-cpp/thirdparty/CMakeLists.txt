add_subdirectory(optional)
add_subdirectory(ordered-map)
add_subdirectory(variant)
add_subdirectory(clsocket)
add_subdirectory(countdownlatch)
add_subdirectory(string-view-lite)
add_subdirectory(CTPL)
set(SPDLOG_BUILD_SHARED ON CACHE BOOL "Build shared library" FORCE)
add_subdirectory(spdlog)

install(DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/spdlog/include/
        DESTINATION "${CMAKE_INSTALL_PUBLIC_HEADER_THIRDPARTY}"
        CONFIGURATIONS Release
        )

add_library(thirdparty STATIC thirdparty.hpp thirdparty.cpp)
target_include_directories(thirdparty PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        optional
        ordered-map
        variant
        string-view-lite
        ctpl
        countdownlatch
        )

target_link_libraries(thirdparty PUBLIC
        optional
        mpark_variant
        tsl::ordered_map
        string_view-lite
        ctpl
        countdownlatch
        spdlog::spdlog
        )

install(FILES thirdparty.hpp
        DESTINATION "${CMAKE_INSTALL_PUBLIC_HEADER_THIRDPARTY}"
        CONFIGURATIONS Release
        )
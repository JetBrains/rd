add_subdirectory(optional)
add_subdirectory(ordered-map)
add_subdirectory(variant)
add_subdirectory(clsocket)
add_subdirectory(string-view-lite)
#add_subdirectory(countdownlatchcpp)

add_library(thirdparty STATIC thirdparty.hpp thirdparty.cpp)
target_include_directories(thirdparty PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        optional
        ordered-map
        variant
        string-view-lite
        )
target_link_libraries(thirdparty optional mpark_variant tsl::ordered_map string_view-lite)

add_subdirectory(optional)
add_subdirectory(ordered-map)
add_subdirectory(variant)
add_subdirectory(clsocket)
add_subdirectory(string-view-lite)
add_subdirectory(memory)
#add_subdirectory(countdownlatchcpp)

add_library(thirdparty STATIC thirdparty.hpp thirdparty.cpp)
target_include_directories(thirdparty PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        optional
        ordered-map
        variant
        string-view-lite
#        memory
#        memory/include/foonathan
        )

target_link_libraries(thirdparty PUBLIC
        optional
        mpark_variant
        tsl::ordered_map
        string_view-lite
#        foonathan_memory
        )
#comp_target_features(thirdparty PUBLIC CPP14)

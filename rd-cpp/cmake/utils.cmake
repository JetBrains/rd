function(copy_shared_dependency_if_needed target)
    foreach(dep IN LISTS ARGN)
        get_target_property(type ${dep} TYPE)
        if(type STREQUAL "SHARED_LIBRARY")
            add_custom_command(TARGET ${target} POST_BUILD
                    COMMAND ${CMAKE_COMMAND} -E copy_if_different
                    $<TARGET_FILE:${dep}> $<TARGET_FILE_DIR:${target}>
                    COMMENT "Copying dependent ${dep} shared lib to target ${target}"
                    VERBATIM)
        endif()
    endforeach()
endfunction()
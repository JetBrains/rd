add_subdirectory(src/main)
if (RD_CORE_CPP_ENABLE_TESTS)
    add_subdirectory(src/test)
endif ()

#add_compile_options(-fno-implicit-templates -ftemplate-backtrace-limit=600)
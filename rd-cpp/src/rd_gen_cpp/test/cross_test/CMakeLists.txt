add_library(cross_test_lib
        CrossTestBase.cpp CrossTestBase.h
        CrossTestClientBase.cpp CrossTestClientBase.h
        CrossTestServerBase.cpp CrossTestServerBase.h
        )

target_link_libraries(cross_test_lib rd_framework_cpp)

add_executable(CrossTestCppClientAllEntities CrossTestCppClientAllEntities.cpp)
add_executable(CrossTestCppClientBigBuffer CrossTestCppClientBigBuffer.cpp)

target_link_libraries(CrossTestCppClientAllEntities cross_test_lib demo_model)
target_link_libraries(CrossTestCppClientBigBuffer cross_test_lib demo_model)
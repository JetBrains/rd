add_library(cross_test_lib
        CrossTestBase.cpp CrossTestBase.h
        CrossTestClientBase.cpp CrossTestClientBase.h
        CrossTestServerBase.cpp CrossTestServerBase.h
        )

target_link_libraries(cross_test_lib rd_framework_cpp)

add_executable(cross_test_client_all_entities CrossTestClientAllEntitites.cpp)
add_executable(cross_test_client_big_buffer CrossTestClientBigBuffer.cpp)

target_link_libraries(cross_test_client_all_entities cross_test_lib demo_model)
target_link_libraries(cross_test_client_big_buffer cross_test_lib demo_model)
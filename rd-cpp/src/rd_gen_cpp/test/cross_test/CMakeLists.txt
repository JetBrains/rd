add_library(cross_test_lib
        CrossTestBase.cpp CrossTestBase.h
        CrossTestClientBase.cpp CrossTestClientBase.h
        CrossTestServerBase.cpp CrossTestServerBase.h
        )

target_link_libraries(cross_test_lib rd_framework_cpp)

add_executable(CrossTestCppClientAllEntities CrossTestCppClientAllEntities.cpp)
add_executable(CrossTestCppClientBigBuffer CrossTestCppClientBigBuffer.cpp)
add_executable(CrossTestCppClientRdCall CrossTestCppClientRdCall.cpp)

add_executable(CrossTestCppServerAllEntities CrossTestCppServerAllEntities.cpp)
add_executable(CrossTestCppServerBigBuffer CrossTestCppServerBigBuffer.cpp)
add_executable(CrossTestCppServerRdCall CrossTestCppServerRdCall.cpp)

foreach(test_target CrossTestCppClientAllEntities CrossTestCppClientBigBuffer CrossTestCppClientRdCall
                    CrossTestCppServerAllEntities CrossTestCppServerBigBuffer CrossTestCppServerRdCall
)
        target_link_libraries("${test_target}" cross_test_lib demo_model)
endforeach()
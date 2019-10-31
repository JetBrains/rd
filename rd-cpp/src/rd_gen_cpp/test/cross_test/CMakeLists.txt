add_library(cross_test_lib
        base/CrossTestBase.cpp base/CrossTestBase.h
        base/CrossTestClientBase.cpp base/CrossTestClientBase.h
        base/CrossTestServerBase.cpp base/CrossTestServerBase.h
        statics/CrossTestAllEntities.cpp statics/CrossTestAllEntities.h
        )
target_include_directories(cross_test_lib PUBLIC base statics)

target_link_libraries(cross_test_lib rd_framework_cpp demo_model)

add_executable(CrossTest_AllEntities_CppClient cases/CrossTest_AllEntities_CppClient.cpp)
add_executable(CrossTest_BigBuffer_CppClient cases/CrossTest_BigBuffer_CppClient.cpp)
add_executable(CrossTest_RdCall_CppClient cases/CrossTest_RdCall_CppClient.cpp)

add_executable(CrossTestCppServerAllEntities cases/CrossTestCppServerAllEntities.cpp)

foreach (test_target
        CrossTest_AllEntities_CppClient CrossTest_BigBuffer_CppClient CrossTest_RdCall_CppClient
        CrossTestCppServerAllEntities
        )
    target_link_libraries("${test_target}" cross_test_lib demo_model)
endforeach (test_target)
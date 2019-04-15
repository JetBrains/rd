cmake_minimum_required(VERSION 3.11)
add_library(interning_test_model STATIC InterningExt/InterningExtRootModel.cpp
        InterningExt/InterningExt.cpp
        InterningExt/InterningExtRootModel.h
        InterningExt/InterningExt.h
        InterningRoot1/InterningTestModel.cpp
        InterningRoot1/InterningNestedTestModel.cpp
        InterningRoot1/InterningNestedTestStringModel.cpp
        InterningRoot1/InterningProtocolLevelModel.cpp
        InterningRoot1/InterningMtModel.cpp
        InterningRoot1/InterningExtensionHolder.cpp
        InterningRoot1/WrappedStringModel.cpp
        InterningRoot1/ProtocolWrappedStringModel.cpp
        InterningRoot1/InterningRoot1.cpp
        InterningRoot1/InterningTestModel.h
        InterningRoot1/InterningNestedTestModel.h
        InterningRoot1/InterningNestedTestStringModel.h
        InterningRoot1/InterningProtocolLevelModel.h
        InterningRoot1/InterningMtModel.h
        InterningRoot1/InterningExtensionHolder.h
        InterningRoot1/WrappedStringModel.h
        InterningRoot1/ProtocolWrappedStringModel.h
        InterningRoot1/InterningRoot1.h)

target_include_directories(interning_test_model PUBLIC . InterningRoot1 InterningExt)
target_link_libraries(interning_test_model PUBLIC rd_framework_cpp)

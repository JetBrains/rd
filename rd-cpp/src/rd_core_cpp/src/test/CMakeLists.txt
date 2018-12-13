#exec

add_executable(rd_core_cpp_test
        cases/SignalTest.cpp
        cases/ViewableMapTest.cpp
        cases/PropertyTest.cpp
        cases/ViewableSetTest.cpp
        cases/AdviseVsViewTest.cpp
        cases/ViewableListTest.cpp
        util/test_util.cpp
        util/test_util.h)

target_include_directories(rd_core_cpp_test PUBLIC cases)
target_include_directories(rd_core_cpp_test PUBLIC util)

target_link_libraries(rd_core_cpp_test gtest gtest_main rd_core_cpp)
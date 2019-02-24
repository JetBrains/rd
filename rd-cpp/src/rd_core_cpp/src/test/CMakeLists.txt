#exec

add_executable(rd_core_cpp_test
        cases/SignalTest.cpp
        cases/ViewableMapTest.cpp
        cases/PropertyTest.cpp
        cases/ViewableSetTest.cpp
        cases/AdviseVsViewTest.cpp
        cases/ViewableListTest.cpp cases/GeneratorUtilTest.cpp)

add_subdirectory(util)

target_include_directories(rd_core_cpp_test PUBLIC cases)
target_include_directories(rd_core_cpp_test PUBLIC util)

target_link_libraries(rd_core_cpp_test gtest gtest_main rd_core_cpp rd_core_cpp_test_util)
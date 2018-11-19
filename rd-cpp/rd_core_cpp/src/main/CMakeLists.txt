#lib

add_library(rd_core_cpp STATIC
        lifetime/LifetimeDefinition.cpp lifetime/LifetimeDefinition.h
        lifetime/LifetimeImpl.cpp lifetime/LifetimeImpl.h
        lifetime/SequentialLifetimes.cpp lifetime/SequentialLifetimes.h
        reactive/SignalX.h
        reactive/interfaces.h
        reactive/Property.h
        reactive/ViewableMap.h
        reactive/ViewableSet.h
        reactive/ViewableList.h
        Logger.cpp Logger.h
        lifetime/Lifetime.cpp lifetime/Lifetime.h
        reactive/IScheduler.cpp reactive/IScheduler.h
        reactive/base/IViewableList.h
        util/core_util.cpp util/core_util.h
        util/gen_util.cpp util/gen_util.h
        reactive/base/IViewableSet.h
        reactive/base/IViewableMap.h reactive/SignalX.cpp
        reactive/base/IProperty.h
        reactive/base/viewable_collections.cpp reactive/base/viewable_collections.h util/overloaded.h util/erase_if.h util/custom_type_traits.h)


target_include_directories(rd_core_cpp PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_include_directories(rd_core_cpp PUBLIC lifetime)
target_include_directories(rd_core_cpp PUBLIC reactive)
target_include_directories(rd_core_cpp PUBLIC util)
target_link_libraries(rd_core_cpp PUBLIC optional mpark_variant)

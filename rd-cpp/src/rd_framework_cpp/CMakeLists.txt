#lib

add_library(rd_framework_cpp STATIC
        #base
        src/base/IProtocol.cpp
        src/base/RdReactiveBase.cpp
        src/base/RdBindableBase.cpp
        src/base/WireBase.cpp
        src/base/WireBase.h
        src/base/ISerializersOwner.cpp
        src/base/IUnknownInstance.cpp
        #impl
        src/impl/RName.cpp
        #ext
        src/ext/RdExtBase.cpp
        src/ext/ExtWire.cpp
        #scheduler
        src/scheduler/SimpleScheduler.cpp
        src/scheduler/SynchronousScheduler.cpp
        #serialization
        src/serialization/SerializationCtx.cpp
        src/serialization/Serializers.cpp
        src/serialization/RdAny.cpp
        src/serialization/DefaultAbstractDeclaration.cpp
        #task
        #wire
        src/wire/SocketWire.cpp
        src/wire/PumpScheduler.cpp
        src/wire/ByteBufferAsyncProcessor.cpp
        #intern
        src/intern/InternRoot.cpp
        src/intern/InternScheduler.cpp
        #others
        src/Identities.cpp
        src/Buffer.cpp
        src/RdId.cpp
        src/Protocol.cpp
        src/MessageBroker.cpp
        #pch
        ${PCH_CPP_OPT}
        )

message(STATUS "Using pch by rd_core: '${ENABLE_PCH_HEADERS}'")

if (ENABLE_PCH_HEADERS)
    include(${PROJECT_SOURCE_DIR}/PrecompiledHeader.cmake)
    add_precompiled_header(rd_framework_cpp ${PCH_H_OPT} SOURCE_CXX ${PCH_CPP_OPT} FORCEINCLUDE)
endif ()

add_subdirectory(util)

target_include_directories(rd_framework_cpp
        PUBLIC
        $<INSTALL_INTERFACE:include>
        $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/src/
        ${CMAKE_CURRENT_SOURCE_DIR}/src/base
        ${CMAKE_CURRENT_SOURCE_DIR}/src/base/ext
        ${CMAKE_CURRENT_SOURCE_DIR}/src/impl
        ${CMAKE_CURRENT_SOURCE_DIR}/src/intern
        ${CMAKE_CURRENT_SOURCE_DIR}/src/scheduler
        ${CMAKE_CURRENT_SOURCE_DIR}/src/serialization
        ${CMAKE_CURRENT_SOURCE_DIR}/src/task
        ${CMAKE_CURRENT_SOURCE_DIR}/src/wire
        #        todo include recursively
        )

target_link_libraries(rd_framework_cpp PUBLIC rd_core_cpp rd_framework_cpp_util rd_gen_cpp clsocket)

if (UNIX)
    target_link_libraries(rd_framework_cpp PRIVATE pthread)
endif ()

if (WIN32)
    if ("${CMAKE_SIZEOF_VOID_P}" STREQUAL "4")
        SET(PLATFORM "x86")
    elseif ("${CMAKE_SIZEOF_VOID_P}" STREQUAL "8")
        SET(PLATFORM "x64")
    endif ()
    install(TARGETS rd_framework_cpp rd_core_cpp clsocket
            CONFIGURATIONS Release
            ARCHIVE
            DESTINATION "${CMAKE_INSTALL_LIBS}/Win/${PLATFORM}/"
            )
endif ()

install(DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        DESTINATION "${CMAKE_INSTALL_PUBLIC_HEADER}/rd_framework_cpp"
        CONFIGURATIONS Release
        FILES_MATCHING
        PATTERN *.h
        PATTERN *.hpp
        )

if (RD_FRAMEWORK_CPP_ENABLE_TESTS)
    add_subdirectory(test)
endif ()

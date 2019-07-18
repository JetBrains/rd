#lib

add_library(rd_framework_cpp STATIC
        #base
        base/IRdBindable.h
        base/IRdReactive.h
        base/IRdDynamic.h
        base/IWire.h
        base/IProtocol.cpp base/IProtocol.h
        base/RdReactiveBase.cpp base/RdReactiveBase.h
        base/RdBindableBase.cpp base/RdBindableBase.h
        base/WireBase.cpp base/WireBase.h
        base/RdPropertyBase.h
        base/ISerializersOwner.cpp
        base/IUnknownInstance.cpp base/IUnknownInstance.h
        #impl
        impl/RdSignal.h
        impl/RdProperty.h
        impl/RdList.h
        impl/RdMap.h
        impl/RName.cpp impl/RName.h
        impl/RdSet.h
        #ext
        ext/RdExtBase.cpp ext/RdExtBase.h
        ext/ExtWire.cpp ext/ExtWire.h
        #scheduler
        scheduler/IScheduler.cpp scheduler/IScheduler.h
        scheduler/SimpleScheduler.cpp scheduler/SimpleScheduler.h
        scheduler/SynchronousScheduler.cpp scheduler/SynchronousScheduler.h
        #serialization
        serialization/SerializationCtx.cpp serialization/SerializationCtx.h
        serialization/Serializers.cpp serialization/Serializers.h
        serialization/Polymorphic.cpp serialization/Polymorphic.h
        serialization/NullableSerializer.h
        serialization/ArraySerializer.h
        serialization/ISerializable.cpp serialization/ISerializable.h
        serialization/InternedSerializer.h
        serialization/AbstractPolymorphic.h
        serialization/RdAny.cpp serialization/RdAny.h
        serialization/InternedAnySerializer.h
        serialization/DefaultAbstractDeclaration.cpp serialization/DefaultAbstractDeclaration.h
        #task
        task/RdCall.h task/RdTask.h task/RdTaskResult.h task/RdEndpoint.h
        task/RdTaskImpl.h
        task/WiredRdTaskImpl.h
        #wire
        wire/SocketWire.cpp wire/SocketWire.h
        wire/PumpScheduler.cpp wire/PumpScheduler.h
        wire/ByteBufferAsyncProcessor.cpp wire/ByteBufferAsyncProcessor.h
        wire/WireUtil.cpp wire/WireUtil.h
        wire/PkgInputStream.cpp wire/PkgInputStream.h
        #intern
        intern/InternRoot.cpp intern/InternRoot.h
        intern/InternScheduler.cpp intern/InternScheduler.h
        #others
        Identities.cpp Identities.h
        Buffer.cpp Buffer.h
        RdId.cpp RdId.h
        Protocol.cpp Protocol.h
        MessageBroker.cpp MessageBroker.h
        #pch
        ${PCH_CPP_OPT}
        base/IRdWireable.cpp base/IRdWireable.h task/WiredRdTask.h task/WiredRdTaskImpl.h task/RdSymmetricCall.cpp task/RdSymmetricCall.h)

message(STATUS "Using pch by rd_framework: '${ENABLE_PCH_HEADERS}'")

if (ENABLE_PCH_HEADERS)
    include(${PROJECT_SOURCE_DIR}/PrecompiledHeader.cmake)
    add_precompiled_header(rd_framework_cpp pch.h SOURCE_CXX pch.cpp FORCEINCLUDE)
endif ()

add_subdirectory(util)

target_include_directories(rd_framework_cpp
        PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        base
        ext
        impl
        intern
        scheduler
        serialization
        task
        wire
        util
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

install(DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/
        DESTINATION "${CMAKE_INSTALL_PUBLIC_HEADER}/rd_framework_cpp"
        CONFIGURATIONS Release
        FILES_MATCHING
        PATTERN *.h
        PATTERN *.hpp
        )

#target_compile_options(rd_framework_cpp PUBLIC -ftemplate-backtrace-limit=100)
#target_link_options(rd_framework_cpp PUBLIC -fdiagnostics-show-template-tree)
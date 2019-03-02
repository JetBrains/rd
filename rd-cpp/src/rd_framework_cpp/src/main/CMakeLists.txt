#lib

add_library(rd_framework_cpp STATIC
        #base
        base/IRdBindable.h
        base/IRdReactive.h
        base/IRdDynamic.h
        base/IWire.h
        base/IIdentities.h
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
        #serialization
        serialization/SerializationCtx.cpp serialization/SerializationCtx.h
        serialization/Serializers.h
        serialization/Polymorphic.h
        serialization/NullableSerializer.h
        serialization/ArraySerializer.h
        serialization/ISerializable.h
        serialization/InternedSerializer.h
        #task
        task/RdCall.h task/RdTask.h task/RdTaskResult.h task/RdEndpoint.h
        task/RdTaskImpl.h
        #wire
        wire/SocketWire.cpp wire/SocketWire.h
        wire/PumpScheduler.cpp wire/PumpScheduler.h
        wire/ByteBufferAsyncProcessor.cpp wire/ByteBufferAsyncProcessor.h
        #intern
#        intern/InternRoot.cpp intern/InternRoot.h
#        intern/InternScheduler.cpp intern/InternScheduler.h

        Identities.cpp Identities.h
        Buffer.cpp Buffer.h
        RdId.cpp RdId.h
        Protocol.cpp Protocol.h
        MessageBroker.cpp MessageBroker.h
        )

target_include_directories(rd_framework_cpp
        PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        base
        ext
        impl
        intern
        serialization
        task
        wire
        util
        )

target_link_libraries(rd_framework_cpp PUBLIC rd_core_cpp clsocket)

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
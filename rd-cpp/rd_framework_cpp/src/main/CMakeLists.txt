#lib

add_library(rd_framework_cpp STATIC
        serialization/SerializationCtx.cpp serialization/SerializationCtx.h
        Identities.cpp Identities.h
        impl/RName.cpp impl/RName.h
        Buffer.cpp Buffer.h
        base/IRdBindable.h
        base/IRdReactive.h
        RdId.cpp RdId.h
        impl/RdSignal.h
        base/RdReactiveBase.cpp base/RdReactiveBase.h
        base/RdBindableBase.cpp base/RdBindableBase.h
        impl/RdProperty.h
        base/ext/RdExtBase.cpp base/ext/RdExtBase.h
        serialization/Serializers.cpp serialization/Serializers.h
        base/WireBase.cpp base/WireBase.h
        Protocol.cpp Protocol.h
        base/RdPropertyBase.h MessageBroker.cpp
        MessageBroker.h
        Buffer.cpp Buffer.h
        serialization/Polymorphic.cpp serialization/Polymorphic.h
        serialization/NullableSerializer.h
        serialization/ArraySerializer.h
        base/IProtocol.cpp base/IProtocol.h
        base/IRdDynamic.h
        base/IWire.h
        base/ISerializer.h
        base/ISerializersOwner.cpp
        base/IIdentities.h
        impl/RdList.h
        impl/RdMap.h
        wire/SocketWire.cpp wire/SocketWire.h
        wire/PumpScheduler.cpp wire/PumpScheduler.h
        impl/RdSet.h
        serialization/ISerializable.cpp serialization/ISerializable.h
        serialization/IMarshaller.cpp serialization/IMarshaller.h
        util/demangle.cpp util/demangle.h
        wire/threading/ByteBufferAsyncProcessor.cpp wire/threading/ByteBufferAsyncProcessor.h
        base/ext/ExtWire.cpp base/ext/ExtWire.h
        util/shared_function.cpp util/shared_function.h
        #task
        task/RdCall.cpp task/RdCall.h task/RdTask.cpp task/RdTask.h task/RdTaskResult.cpp task/RdTaskResult.h task/RdEndpoint.cpp task/RdEndpoint.h
        task/RdTaskImpl.cpp task/RdTaskImpl.h
        base/IUnknownInstance.cpp base/IUnknownInstance.h)
target_include_directories(rd_framework_cpp
        PUBLIC
                ${CMAKE_CURRENT_SOURCE_DIR}
                base
                base/ext
                impl
                serialization
                util
                wire
                task
)

target_link_libraries(rd_framework_cpp PUBLIC rd_core_cpp clsocket)

if(UNIX)
        target_link_libraries(rd_framework_cpp PRIVATE pthread)
endif()

if(WIN32)
        if("${CMAKE_SIZEOF_VOID_P}" STREQUAL "4")
                SET(PLATFORM "x86")
        elseif("${CMAKE_SIZEOF_VOID_P}" STREQUAL "8")
                SET(PLATFORM "x64")
        endif()
        #[[install(TARGETS rd_framework_cpp rd_core_cpp clsocket
                CONFIGURATIONS Release
                ARCHIVE
                        DESTINATION "${CMAKE_INSTALL_LIBS}/Win/${PLATFORM}/"
        )]]
endif()

install(DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/
        DESTINATION "${CMAKE_INSTALL_PUBLIC_HEADER}/rd_framework_cpp"
        CONFIGURATIONS Release
        FILES_MATCHING
                PATTERN *.h
                PATTERN *.hpp
)
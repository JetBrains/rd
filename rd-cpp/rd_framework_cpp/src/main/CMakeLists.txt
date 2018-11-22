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
        )

target_include_directories(rd_framework_cpp PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_include_directories(rd_framework_cpp PUBLIC base)
target_include_directories(rd_framework_cpp PUBLIC impl)
target_include_directories(rd_framework_cpp PUBLIC serialization)
target_include_directories(rd_framework_cpp PUBLIC util)
target_include_directories(rd_framework_cpp PUBLIC wire)
target_include_directories(rd_framework_cpp PUBLIC task)
target_include_directories(rd_framework_cpp PUBLIC base/ext)

add_subdirectory(wire)

target_link_libraries(rd_framework_cpp PUBLIC rd_core_cpp clsocket)

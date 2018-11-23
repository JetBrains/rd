#ifndef UnrealEngineModel_H
#define UnrealEngineModel_H

#include "Buffer.h" 
#include "Identities.h" 
#include "MessageBroker.h" 
#include "Protocol.h" 
#include "RdId.h" 
#include "RdList.h" 
#include "RdMap.h" 
#include "RdProperty.h" 
#include "RdSet.h" 
#include "RdSignal.h" 
#include "RName.h" 
#include "IMarshaller.h" 
#include "ISerializable.h" 
#include "Polymorphic.h" 
#include "NullableSerializer.h" 
#include "ArraySerializer.h" 
#include "SerializationCtx.h" 
#include "Serializers.h" 
#include "ISerializersOwner.h" 
#include "IUnknownInstance.h" 
#include "RdExtBase.h" 
#include "RdCall.h" 
#include "RdEndpoint.h" 
#include "RdTask.h" 
#include "RdTaskResult.h" 
#include "gen_util.h" 

#include <iostream>
#include <cstring>
#include <cstdint>
#include <vector>
#include <type_traits>
#include <optional>
#include <utility>


class UnrealEngineModel : public RdExtBase
{
    
    //companion
    
    public:
    struct UnrealEngineModelSerializersOwner : public ISerializersOwner {
        void registerSerializersCore(Serializers const& serializers) override;
    };
    
    static UnrealEngineModelSerializersOwner serializersOwner;
    
    
    public:
    static UnrealEngineModel create(Lifetime lifetime, IProtocol * protocol);
    
    
    //custom serializers
    private:
    using __IntNullableSerializer = NullableSerializer<Polymorphic<int32_t>>;
    using __StringNullableSerializer = NullableSerializer<Polymorphic<std::wstring>>;
    
    //fields
    protected:
    RdProperty<tl::optional<int32_t>, UnrealEngineModel::__IntNullableSerializer> _test_connection{tl::nullopt};
    RdProperty<tl::optional<std::wstring>, UnrealEngineModel::__StringNullableSerializer> _filename_to_open{tl::nullopt};
    
    
    //initializer
    private:
    void init();
    
    //primary ctor
    private:
    explicit UnrealEngineModel(RdProperty<tl::optional<int32_t>, UnrealEngineModel::__IntNullableSerializer> _test_connection, RdProperty<tl::optional<std::wstring>, UnrealEngineModel::__StringNullableSerializer> _filename_to_open);
    
    //default ctors and dtors
    public:
    
    UnrealEngineModel() {
        init();
    }
    
    UnrealEngineModel(UnrealEngineModel &&) = default;
    
    UnrealEngineModel& operator=(UnrealEngineModel &&) = default;
    
    virtual ~UnrealEngineModel() = default;
    
    //reader
    public:
    
    //writer
    public:
    
    //getters
    public:
    RdProperty<tl::optional<int32_t>, UnrealEngineModel::__IntNullableSerializer> const & get_test_connection() const;
    RdProperty<tl::optional<std::wstring>, UnrealEngineModel::__StringNullableSerializer> const & get_filename_to_open() const;
    
    //equals trait
    
    //hash code trait
};

//hash code trait

#endif // UnrealEngineModel_H

#ifndef UnrealEngineModel_H
#define UnrealEngineModel_H

#include <iostream>
#include <cstring>
#include <cstdint>
#include <vector>
#include <type_traits>
#include <optional>
#include <utility>

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
#include "RdExtBase.h" 
#include "RdCall.h" 
#include "RdEndpoint.h" 
#include "RdTask.h" 
#include "RdTaskResult.h" 
#include "gen_util.h" 


class UnrealEngineModel : public RdExtBase
{
    
    //companion
    
    private:
    struct UnrealEngineModelSerializersOwner : public ISerializersOwner {
        void registerSerializersCore(Serializers const& serializers) override;
    };
    
    public:
    static UnrealEngineModelSerializersOwner serializersOwner;
    
    
    public:
    
    //create method
    static UnrealEngineModel create(Lifetime lifetime, IProtocol * protocol);
    
    
    //custom serializers
    private:
    using __IntNullableSerializer = NullableSerializer<Polymorphic<int32_t>>;
    using __StringNullableSerializer = NullableSerializer<Polymorphic<std::string>>;
    
    //fields
    protected:
    RdProperty<tl::optional<int32_t>, UnrealEngineModel::__IntNullableSerializer> _test_connection{tl::nullopt};
    RdProperty<tl::optional<std::string>, UnrealEngineModel::__StringNullableSerializer> _test_string{tl::nullopt};
    
    
    //initializer
    private:
    void init();
    
    //primary ctor
    private:
    explicit UnrealEngineModel(RdProperty<tl::optional<int32_t>, UnrealEngineModel::__IntNullableSerializer> _test_connection, RdProperty<tl::optional<std::string>, UnrealEngineModel::__StringNullableSerializer> _test_string);
    
    //default ctors and dtors
    public:
    
    UnrealEngineModel() = default;
    
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
    RdProperty<tl::optional<std::string>, UnrealEngineModel::__StringNullableSerializer> const & get_test_string() const;
    
    //equals trait
    
    //hash code trait
};

//hash code trait

#endif // UnrealEngineModel_H

#ifndef AbstractEntity_H
#define AbstractEntity_H

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


#pragma warning( push )
#pragma warning( disable:4250 )

//abstract
class AbstractEntity : public ISerializable
{
    //fields
    protected:
    std::wstring filePath;
    
    
    //initializer
    private:
    void initialize();
    
    //primary ctor
    public:
    explicit AbstractEntity(std::wstring filePath);
    
    //default ctors and dtors
    
    AbstractEntity() {
        initialize();
    };
    
    AbstractEntity(AbstractEntity const &) = default;
    
    AbstractEntity& operator=(AbstractEntity const &) = default;
    
    AbstractEntity(AbstractEntity &&) = default;
    
    AbstractEntity& operator=(AbstractEntity &&) = default;
    
    virtual ~AbstractEntity() = default;
    
    //reader
    static rd::Wrapper<AbstractEntity> readUnknownInstance(SerializationCtx const& ctx, Buffer const & buffer, RdId const& unknownId, int32_t size);
    
    //writer
    virtual void write(SerializationCtx const& ctx, Buffer const& buffer) const = 0;
    
    //virtual init
    
    //identify
    
    //getters
    std::wstring const & get_filePath() const;
    
    //equals trait
    virtual bool equals(ISerializable const& object) const = 0;
    
    //equality operators
    friend bool operator==(const AbstractEntity &lhs, const AbstractEntity &rhs);
    friend bool operator!=(const AbstractEntity &lhs, const AbstractEntity &rhs);
    
    //hash code trait
    virtual size_t hashCode() const = 0;
};

#pragma warning( pop )


//hash code trait
namespace std {
    template <> struct hash<AbstractEntity> {
        size_t operator()(const AbstractEntity & value) const {
            return value.hashCode();
        }
    };
}

#endif // AbstractEntity_H

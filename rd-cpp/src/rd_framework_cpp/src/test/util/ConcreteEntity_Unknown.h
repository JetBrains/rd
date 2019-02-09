#ifndef ConcreteEntity_Unknown_H
#define ConcreteEntity_Unknown_H

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

#include "AbstractEntity.h"


#pragma warning( push )
#pragma warning( disable:4250 )
class ConcreteEntity_Unknown : public AbstractEntity, public IUnknownInstance
{
    
    //companion
    
    //custom serializers
    private:
    
    //fields
    protected:
    RdId unknownId;
    Buffer::ByteArray unknownBytes;
    
    
    //initializer
    private:
    void initialize();
    
    //primary ctor
    public:
    explicit ConcreteEntity_Unknown(std::wstring filePath, RdId unknownId, Buffer::ByteArray unknownBytes);
    
    //default ctors and dtors
    
    ConcreteEntity_Unknown() {
        initialize();
    };
    
    ConcreteEntity_Unknown(ConcreteEntity_Unknown const &) = default;
    
    ConcreteEntity_Unknown& operator=(ConcreteEntity_Unknown const &) = default;
    
    ConcreteEntity_Unknown(ConcreteEntity_Unknown &&) = default;
    
    ConcreteEntity_Unknown& operator=(ConcreteEntity_Unknown &&) = default;
    
    virtual ~ConcreteEntity_Unknown() = default;
    
    //reader
    static ConcreteEntity_Unknown read(SerializationCtx const& ctx, Buffer const & buffer);
    
    //writer
    void write(SerializationCtx const& ctx, Buffer const& buffer) const override;
    
    //virtual init
    
    //identify
    
    //getters
    
    //equals trait
    bool equals(ISerializable const& object) const override;
    
    //equality operators
    friend bool operator==(const ConcreteEntity_Unknown &lhs, const ConcreteEntity_Unknown &rhs);
    friend bool operator!=(const ConcreteEntity_Unknown &lhs, const ConcreteEntity_Unknown &rhs);
    
    //hash code trait
    size_t hashCode() const override;
};

#pragma warning( pop )


//hash code trait
namespace std {
    template <> struct hash<ConcreteEntity_Unknown> {
        size_t operator()(const ConcreteEntity_Unknown & value) const {
            return value.hashCode();
        }
    };
}

#endif // ConcreteEntity_Unknown_H

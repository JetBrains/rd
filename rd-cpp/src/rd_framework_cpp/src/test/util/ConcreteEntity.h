#ifndef ConcreteEntity_H
#define ConcreteEntity_H

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
class ConcreteEntity : public AbstractEntity
{
    
    //companion
    
    //custom serializers
    private:
    
    //fields
    protected:
    
    //initializer
    private:
    void initialize();
    
    //primary ctor
    public:
    explicit ConcreteEntity(std::wstring filePath);
    
    //default ctors and dtors
    
    ConcreteEntity() {
        initialize();
    };
    
    ConcreteEntity(ConcreteEntity const &) = default;
    
    ConcreteEntity& operator=(ConcreteEntity const &) = default;
    
    ConcreteEntity(ConcreteEntity &&) = default;
    
    ConcreteEntity& operator=(ConcreteEntity &&) = default;
    
    virtual ~ConcreteEntity() = default;
    
    //reader
    static ConcreteEntity read(SerializationCtx const& ctx, Buffer const & buffer);
    
    //writer
    void write(SerializationCtx const& ctx, Buffer const& buffer) const override;
    
    //virtual init
    
    //identify
    
    //getters
    
    //equals trait
    bool equals(ISerializable const& object) const override;
    
    //equality operators
    friend bool operator==(const ConcreteEntity &lhs, const ConcreteEntity &rhs);
    friend bool operator!=(const ConcreteEntity &lhs, const ConcreteEntity &rhs);
    
    //hash code trait
    size_t hashCode() const override;
};

#pragma warning( pop )


//hash code trait
namespace std {
    template <> struct hash<ConcreteEntity> {
        size_t operator()(const ConcreteEntity & value) const {
            return value.hashCode();
        }
    };
}

#endif // ConcreteEntity_H

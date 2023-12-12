//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by a RdGen v1.13.
//
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------
#ifndef BASECLASSWITHINTERFACE_UNKNOWN_GENERATED_H
#define BASECLASSWITHINTERFACE_UNKNOWN_GENERATED_H


#include "protocol/Protocol.h"
#include "types/DateTime.h"
#include "impl/RdSignal.h"
#include "impl/RdProperty.h"
#include "impl/RdList.h"
#include "impl/RdSet.h"
#include "impl/RdMap.h"
#include "base/ISerializersOwner.h"
#include "base/IUnknownInstance.h"
#include "serialization/ISerializable.h"
#include "serialization/Polymorphic.h"
#include "serialization/NullableSerializer.h"
#include "serialization/ArraySerializer.h"
#include "serialization/InternedSerializer.h"
#include "serialization/SerializationCtx.h"
#include "serialization/Serializers.h"
#include "ext/RdExtBase.h"
#include "task/RdCall.h"
#include "task/RdEndpoint.h"
#include "task/RdSymmetricCall.h"
#include "std/to_string.h"
#include "std/hash.h"
#include "std/allocator.h"
#include "util/enum.h"
#include "util/gen_util.h"

#include <cstring>
#include <cstdint>
#include <vector>
#include <ctime>

#include "thirdparty.hpp"
#include "instantiations_ExampleRootNova.h"

#include "BaseClassWithInterface.Generated.h"



#ifdef _MSC_VER
#pragma warning( push )
#pragma warning( disable:4250 )
#pragma warning( disable:4307 )
#pragma warning( disable:4267 )
#pragma warning( disable:4244 )
#pragma warning( disable:4100 )
#endif

namespace org.example {

class BaseClassWithInterface_Unknown : public BaseClassWithInterface, public rd::IUnknownInstance {
friend class BaseClassWithInterface;

private:
    // custom serializers

public:
    // constants

protected:
    // fields
    rd::RdId unknownId_;
    rd::Buffer::ByteArray unknownBytes_;
    

private:
    // initializer
    void initialize();

public:
    // primary ctor
    BaseClassWithInterface_Unknown(rd::RdId unknownId_, rd::Buffer::ByteArray unknownBytes_);
    
    // default ctors and dtors
    
    BaseClassWithInterface_Unknown();
    
    BaseClassWithInterface_Unknown(BaseClassWithInterface_Unknown &&) = default;
    
    BaseClassWithInterface_Unknown& operator=(BaseClassWithInterface_Unknown &&) = default;
    
    virtual ~BaseClassWithInterface_Unknown() = default;
    
    // reader
    static BaseClassWithInterface_Unknown read(rd::SerializationCtx& ctx, rd::Buffer & buffer);
    
    // writer
    void write(rd::SerializationCtx& ctx, rd::Buffer& buffer) const override;
    
    // virtual init
    void init(rd::Lifetime lifetime) const override;
    
    // identify
    void identify(const rd::Identities &identities, rd::RdId const &id) const override;
    
    // getters
    
    // intern

private:
    // equals trait
    bool equals(rd::ISerializable const& object) const override;

public:
    // equality operators
    friend bool operator==(const BaseClassWithInterface_Unknown &lhs, const BaseClassWithInterface_Unknown &rhs);
    friend bool operator!=(const BaseClassWithInterface_Unknown &lhs, const BaseClassWithInterface_Unknown &rhs);
    // hash code trait
    // type name trait
    std::string type_name() const override;
    // static type name trait
    static std::string static_type_name();

private:
    // polymorphic to string
    std::string toString() const override;

public:
    // external to string
    friend std::string to_string(const BaseClassWithInterface_Unknown & value);
};

}

#ifdef _MSC_VER
#pragma warning( pop )
#endif



#endif // BASECLASSWITHINTERFACE_UNKNOWN_GENERATED_H

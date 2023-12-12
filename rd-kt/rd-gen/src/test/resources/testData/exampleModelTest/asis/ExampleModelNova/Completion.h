//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by a RdGen v1.13.
//
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------
#ifndef COMPLETION_GENERATED_H
#define COMPLETION_GENERATED_H


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



#ifdef _MSC_VER
#pragma warning( push )
#pragma warning( disable:4250 )
#pragma warning( disable:4307 )
#pragma warning( disable:4267 )
#pragma warning( disable:4244 )
#pragma warning( disable:4100 )
#endif

/// <summary>
/// <p>Generated from: Example.kt:157</p>
/// </summary>
namespace org.example {

class Completion : public rd::IPolymorphicSerializable, public rd::RdBindableBase {

private:
    // custom serializers

public:
    // constants

protected:
    // fields
    rd::RdMap<int32_t, bool, rd::Polymorphic<int32_t>, rd::Polymorphic<bool>> lookupItems_;
    

private:
    // initializer
    void initialize();

public:
    // primary ctor
    explicit Completion(rd::RdMap<int32_t, bool, rd::Polymorphic<int32_t>, rd::Polymorphic<bool>> lookupItems_);
    
    // default ctors and dtors
    
    Completion();
    
    Completion(Completion &&) = default;
    
    Completion& operator=(Completion &&) = default;
    
    virtual ~Completion() = default;
    
    // reader
    static Completion read(rd::SerializationCtx& ctx, rd::Buffer & buffer);
    
    // writer
    void write(rd::SerializationCtx& ctx, rd::Buffer& buffer) const override;
    
    // virtual init
    void init(rd::Lifetime lifetime) const override;
    
    // identify
    void identify(const rd::Identities &identities, rd::RdId const &id) const override;
    
    // getters
    rd::IViewableMap<int32_t, bool> const & get_lookupItems() const;
    
    // intern

private:
    // equals trait
    bool equals(rd::ISerializable const& object) const override;

public:
    // equality operators
    friend bool operator==(const Completion &lhs, const Completion &rhs);
    friend bool operator!=(const Completion &lhs, const Completion &rhs);
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
    friend std::string to_string(const Completion & value);
};

}

#ifdef _MSC_VER
#pragma warning( pop )
#endif



#endif // COMPLETION_GENERATED_H

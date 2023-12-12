//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by a RdGen v1.13.
//
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------
#ifndef SCALAREXAMPLE_GENERATED_H
#define SCALAREXAMPLE_GENERATED_H


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
/// <p>Generated from: Example.kt:166</p>
/// </summary>
namespace org.example {

// data
class ScalarExample : public rd::IPolymorphicSerializable {

private:
    // custom serializers

public:
    // constants

protected:
    // fields
    int32_t intfield_;
    

private:
    // initializer
    void initialize();

public:
    // primary ctor
    explicit ScalarExample(int32_t intfield_);
    
    // deconstruct trait
    #ifdef __cpp_structured_bindings
    template <size_t I>
    decltype(auto) get() const
    {
        if constexpr (I < 0 || I >= 1) static_assert (I < 0 || I >= 1, "I < 0 || I >= 1");
        else if constexpr (I==0)  return static_cast<const int32_t&>(get_intfield());
    }
    #endif
    
    // default ctors and dtors
    
    ScalarExample() = delete;
    
    ScalarExample(ScalarExample const &) = default;
    
    ScalarExample& operator=(ScalarExample const &) = default;
    
    ScalarExample(ScalarExample &&) = default;
    
    ScalarExample& operator=(ScalarExample &&) = default;
    
    virtual ~ScalarExample() = default;
    
    // reader
    static ScalarExample read(rd::SerializationCtx& ctx, rd::Buffer & buffer);
    
    // writer
    void write(rd::SerializationCtx& ctx, rd::Buffer& buffer) const override;
    
    // virtual init
    
    // identify
    
    // getters
    int32_t const & get_intfield() const;
    
    // intern

private:
    // equals trait
    bool equals(rd::ISerializable const& object) const override;

public:
    // equality operators
    friend bool operator==(const ScalarExample &lhs, const ScalarExample &rhs);
    friend bool operator!=(const ScalarExample &lhs, const ScalarExample &rhs);
    // hash code trait
    size_t hashCode() const noexcept override;
    // type name trait
    std::string type_name() const override;
    // static type name trait
    static std::string static_type_name();

private:
    // polymorphic to string
    std::string toString() const override;

public:
    // external to string
    friend std::string to_string(const ScalarExample & value);
};

}

// hash code trait
namespace rd {

template <>
struct hash<org.example::ScalarExample> {
    size_t operator()(const org.example::ScalarExample & value) const noexcept {
        return value.hashCode();
    }
};

}

#ifdef __cpp_structured_bindings
// tuple trait
namespace std {

template <>
class tuple_size<org.example::ScalarExample> : public integral_constant<size_t, 1> {};

template <size_t I>
class tuple_element<I, org.example::ScalarExample> {
public:
    using type = decltype (declval<org.example::ScalarExample>().get<I>());
};

}
#endif

#ifdef _MSC_VER
#pragma warning( pop )
#endif



#endif // SCALAREXAMPLE_GENERATED_H

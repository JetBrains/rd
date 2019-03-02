#ifndef MyScalar_H
#define MyScalar_H

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
#include "gen_util.h"

#include <iostream>
#include <cstring>
#include <cstdint>
#include <vector>
#include <type_traits>
#include <utility>

#include "optional.hpp"


#pragma warning( push )
#pragma warning( disable:4250 )

//data
class MyScalar : public rd::IPolymorphicSerializable
{
    
    //companion
    
    //custom serializers
    private:
    
    //fields
    protected:
    bool sign_;
    signed char byte_;
    short short_;
    int32_t int_;
    int64_t long_;
    
    
    //initializer
    private:
    void initialize();
    
    //primary ctor
    public:
    explicit MyScalar(bool sign_, signed char byte_, short short_, int32_t int_, int64_t long_);
    
    
    //default ctors and dtors
    
    MyScalar() {
        initialize();
    };
    
    MyScalar(MyScalar const &) = default;
    
    MyScalar& operator=(MyScalar const &) = default;
    
    MyScalar(MyScalar &&) = default;
    
    MyScalar& operator=(MyScalar &&) = default;
    
    virtual ~MyScalar() = default;
    
    //reader
    static MyScalar read(rd::SerializationCtx const& _ctx, rd::Buffer const & buffer);
    
    //writer
    void write(rd::SerializationCtx const& _ctx, rd::Buffer const& buffer) const override;
    
    //virtual init
    
    //identify
    
    //getters
    bool const & get_sign() const;
    signed char const & get_byte() const;
    short const & get_short() const;
    int32_t const & get_int() const;
    int64_t const & get_long() const;
    
    //intern
    
    //equals trait
    private:
    bool equals(rd::IPolymorphicSerializable const& object) const;
    
    //equality operators
    public:
    friend bool operator==(const MyScalar &lhs, const MyScalar &rhs);
    friend bool operator!=(const MyScalar &lhs, const MyScalar &rhs);
    
    //hash code trait
    size_t hashCode() const;
    
    //type name trait
    std::string type_name() const override;
};

#pragma warning( pop )


//hash code trait
namespace std {
    template <> struct hash<MyScalar> {
        size_t operator()(const MyScalar & value) const {
            return value.hashCode();
        }
    };
}

#endif // MyScalar_H

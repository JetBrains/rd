#ifndef MyScalar_H
#define MyScalar_H

#include "Buffer.h"
#include "Identities.h"
#include "Protocol.h"
#include "RdId.h"
#include "RdSignal.h"
#include "RdProperty.h"
#include "RdList.h"
#include "RdSet.h"
#include "RdMap.h"
#include "ISerializable.h"
#include "ISerializersOwner.h"
#include "IUnknownInstance.h"
#include "Polymorphic.h"
#include "NullableSerializer.h"
#include "ArraySerializer.h"
#include "InternedSerializer.h"
#include "SerializationCtx.h"
#include "Serializers.h"
#include "RdExtBase.h"
#include "RdCall.h"
#include "RdEndpoint.h"
#include "RdTask.h"
#include "gen_util.h"

#include <cstring>
#include <cstdint>
#include <vector>

#include "thirdparty.hpp"
#include "instantiations.h"

#include "MyEnum.h"


#pragma warning( push )
#pragma warning( disable:4250 )
#pragma warning( disable:4307 )
#pragma warning( disable:4267 )
#pragma warning( disable:4244 )
namespace demo {
    
    //data
    class MyScalar : public rd::IPolymorphicSerializable
    {
        
        //companion
        
        //custom serializers
        private:
        
        //constants
        public:
        static constexpr int32_t const_int = 0;
        static constexpr rd::wstring_view const_string = L"const_string_value";
        static constexpr MyEnum const_enum = MyEnum::default_;
        
        //fields
        protected:
        bool bool_;
        uint8_t byte_;
        int16_t short_;
        int32_t int_;
        int64_t long_;
        float float_;
        double double_;
        uint8_t unsigned_byte_;
        uint16_t unsigned_short_;
        uint32_t unsigned_int_;
        uint64_t unsigned_long_;
        MyEnum enum_;
        
        
        //initializer
        private:
        void initialize();
        
        //primary ctor
        public:
        MyScalar(bool bool_, uint8_t byte_, int16_t short_, int32_t int_, int64_t long_, float float_, double double_, uint8_t unsigned_byte_, uint16_t unsigned_short_, uint32_t unsigned_int_, uint64_t unsigned_long_, MyEnum enum_);
        
        //secondary constructor
        
        //default ctors and dtors
        
        MyScalar() = delete;
        
        MyScalar(MyScalar const &) = default;
        
        MyScalar& operator=(MyScalar const &) = default;
        
        MyScalar(MyScalar &&) = default;
        
        MyScalar& operator=(MyScalar &&) = default;
        
        virtual ~MyScalar() = default;
        
        //reader
        static MyScalar read(rd::SerializationCtx& ctx, rd::Buffer & buffer);
        
        //writer
        void write(rd::SerializationCtx& ctx, rd::Buffer& buffer) const override;
        
        //virtual init
        
        //identify
        
        //getters
        bool const & get_bool() const;
        uint8_t const & get_byte() const;
        int16_t const & get_short() const;
        int32_t const & get_int() const;
        int64_t const & get_long() const;
        float const & get_float() const;
        double const & get_double() const;
        uint8_t const & get_unsigned_byte() const;
        uint16_t const & get_unsigned_short() const;
        uint32_t const & get_unsigned_int() const;
        uint64_t const & get_unsigned_long() const;
        MyEnum const & get_enum() const;
        
        //intern
        
        //equals trait
        private:
        bool equals(rd::ISerializable const& object) const override;
        
        //equality operators
        public:
        friend bool operator==(const MyScalar &lhs, const MyScalar &rhs);
        friend bool operator!=(const MyScalar &lhs, const MyScalar &rhs);
        
        //hash code trait
        size_t hashCode() const noexcept override;
        
        //type name trait
        std::string type_name() const override;
        
        //static type name trait
        static std::string static_type_name();
        
        //polymorphic to string
        private:
        std::string toString() const override;
        
        //external to string
        public:
        friend std::string to_string(const MyScalar & value);
    };
};

#pragma warning( pop )


//hash code trait
namespace std {
    template <> struct hash<demo::MyScalar> {
        size_t operator()(const demo::MyScalar & value) const noexcept {
            return value.hashCode();
        }
    };
}

#endif // MyScalar_H

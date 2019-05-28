#ifndef Base_H
#define Base_H

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


#pragma warning( push )
#pragma warning( disable:4250 )
#pragma warning( disable:4307 )
#pragma warning( disable:4267 )
#pragma warning( disable:4244 )
namespace demo {
    
    //abstract
    class Base : public rd::IPolymorphicSerializable
    {
        
        //companion
        
        //custom serializers
        private:
        
        //constants
        public:
        static constexpr wchar_t const_base = L'B';
        
        //fields
        protected:
        
        //initializer
        private:
        void initialize();
        
        //primary ctor
        public:
        
        //secondary constructor
        
        //default ctors and dtors
        
        Base();
        
        Base(Base const &) = default;
        
        Base& operator=(Base const &) = default;
        
        Base(Base &&) = default;
        
        Base& operator=(Base &&) = default;
        
        virtual ~Base() = default;
        
        //reader
        static rd::Wrapper<Base> readUnknownInstance(rd::SerializationCtx& ctx, rd::Buffer & buffer, rd::RdId const& unknownId, int32_t size);
        
        //writer
        virtual void write(rd::SerializationCtx& ctx, rd::Buffer& buffer) const override = 0;
        
        //virtual init
        
        //identify
        
        //getters
        
        //intern
        
        //equals trait
        private:
        
        //equality operators
        public:
        friend bool operator==(const Base &lhs, const Base &rhs);
        friend bool operator!=(const Base &lhs, const Base &rhs);
        
        //hash code trait
        virtual size_t hashCode() const noexcept override = 0;
        
        //type name trait
        std::string type_name() const override;
        
        //static type name trait
        static std::string static_type_name();
        
        //polymorphic to string
        private:
        std::string toString() const override;
        
        //external to string
        public:
        friend std::string to_string(const Base & value);
    };
};

#pragma warning( pop )


//hash code trait
namespace std {
    template <> struct hash<demo::Base> {
        size_t operator()(const demo::Base & value) const noexcept {
            return value.hashCode();
        }
    };
}

#endif // Base_H

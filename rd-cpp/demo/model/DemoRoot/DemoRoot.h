#ifndef DemoRoot_H
#define DemoRoot_H

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
namespace demo {
    class DemoRoot : public rd::RdExtBase
    {
        
        //companion
        
        public:
        struct DemoRootSerializersOwner : public rd::ISerializersOwner {
            void registerSerializersCore(rd::Serializers const& serializers) const override;
        };
        
        static const DemoRootSerializersOwner serializersOwner;
        
        
        public:
        void connect(rd::Lifetime lifetime, rd::IProtocol const * protocol);
        
        
        //custom serializers
        private:
        
        //fields
        protected:
        
        //initializer
        private:
        void initialize();
        
        //primary ctor
        public:
        
        //secondary constructor
        
        //default ctors and dtors
        
        DemoRoot();
        
        DemoRoot(DemoRoot &&) = delete;
        
        DemoRoot& operator=(DemoRoot &&) = delete;
        
        virtual ~DemoRoot() = default;
        
        //reader
        
        //writer
        
        //virtual init
        void init(rd::Lifetime lifetime) const override;
        
        //identify
        void identify(const rd::Identities &identities, rd::RdId const &id) const override;
        
        //getters
        
        //intern
        
        //equals trait
        private:
        
        //equality operators
        public:
        friend bool operator==(const DemoRoot &lhs, const DemoRoot &rhs);
        friend bool operator!=(const DemoRoot &lhs, const DemoRoot &rhs);
        
        //hash code trait
        
        //type name trait
        
        //static type name trait
        
        //polymorphic to string
        private:
        std::string toString() const override;
        
        //external to string
        public:
        friend std::string to_string(const DemoRoot & value);
    };
};

#pragma warning( pop )


//hash code trait

#endif // DemoRoot_H

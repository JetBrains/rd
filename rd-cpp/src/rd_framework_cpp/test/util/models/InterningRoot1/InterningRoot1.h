#ifndef InterningRoot1_H
#define InterningRoot1_H

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
#include "InternedSerializer.h"
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
#pragma warning( disable:4307 )
namespace rd {
    namespace test {
        namespace util {
            class InterningRoot1 : public rd::RdExtBase
            {
                
                //companion
                
                public:
                struct InterningRoot1SerializersOwner : public rd::ISerializersOwner {
                    void registerSerializersCore(rd::Serializers const& serializers) const override;
                };
                
                static const InterningRoot1SerializersOwner serializersOwner;
                
                
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
                
                InterningRoot1();
                
                InterningRoot1(InterningRoot1 &&) = delete;
                
                InterningRoot1& operator=(InterningRoot1 &&) = delete;
                
                virtual ~InterningRoot1() = default;
                
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
                friend bool operator==(const InterningRoot1 &lhs, const InterningRoot1 &rhs);
                friend bool operator!=(const InterningRoot1 &lhs, const InterningRoot1 &rhs);
                
                //hash code trait
                
                //type name trait
                
                //static type name trait
            };
        };
    };
};

#pragma warning( pop )


//hash code trait

#endif // InterningRoot1_H

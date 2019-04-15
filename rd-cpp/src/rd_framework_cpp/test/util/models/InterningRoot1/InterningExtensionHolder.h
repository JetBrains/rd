#ifndef InterningExtensionHolder_H
#define InterningExtensionHolder_H

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
            class InterningExtensionHolder : public rd::IPolymorphicSerializable, public rd::RdBindableBase
            {
                
                //companion
                
                //custom serializers
                private:
                
                //fields
                protected:
                mutable optional<rd::SerializationCtx> mySerializationContext;
                
                //initializer
                private:
                void initialize();
                
                //primary ctor
                public:
                
                //secondary constructor
                
                //default ctors and dtors
                
                InterningExtensionHolder();
                
                InterningExtensionHolder(InterningExtensionHolder &&) = default;
                
                InterningExtensionHolder& operator=(InterningExtensionHolder &&) = default;
                
                virtual ~InterningExtensionHolder() = default;
                
                //reader
                static InterningExtensionHolder read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer);
                
                //writer
                void write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const override;
                
                //virtual init
                void init(rd::Lifetime lifetime) const override;
                
                //identify
                void identify(const rd::Identities &identities, rd::RdId const &id) const override;
                
                //getters
                
                //intern
                const rd::SerializationCtx & get_serialization_context() const override;
                
                //equals trait
                private:
                bool equals(rd::ISerializable const& object) const override;
                
                //equality operators
                public:
                friend bool operator==(const InterningExtensionHolder &lhs, const InterningExtensionHolder &rhs);
                friend bool operator!=(const InterningExtensionHolder &lhs, const InterningExtensionHolder &rhs);
                
                //hash code trait
                
                //type name trait
                std::string type_name() const override;
                
                //static type name trait
                static std::string static_type_name();
            };
        };
    };
};

#pragma warning( pop )


//hash code trait

#endif // InterningExtensionHolder_H

#ifndef InterningExt_H
#define InterningExt_H

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

#include "InterningExtRootModel.h"
#include "InterningExtensionHolder.h"


#pragma warning( push )
#pragma warning( disable:4250 )
#pragma warning( disable:4307 )
namespace rd {
    namespace test {
        namespace util {
            class InterningExt : public rd::RdExtBase
            {
                
                //companion
                
                public:
                struct InterningExtSerializersOwner : public rd::ISerializersOwner {
                    void registerSerializersCore(rd::Serializers const& serializers) const override;
                };
                
                static const InterningExtSerializersOwner serializersOwner;
                
                
                public:
                
                
                //extension
                static InterningExt const & getOrCreateExtensionOf(InterningExtensionHolder & pointcut);
                
                //custom serializers
                private:
                
                //fields
                protected:
                rd::RdProperty<InterningExtRootModel, rd::Polymorphic<InterningExtRootModel>> root_;
                
                
                //initializer
                private:
                void initialize();
                
                //primary ctor
                public:
                InterningExt(rd::RdProperty<InterningExtRootModel, rd::Polymorphic<InterningExtRootModel>> root_);
                
                //secondary constructor
                
                //default ctors and dtors
                
                InterningExt();
                
                InterningExt(InterningExt &&) = delete;
                
                InterningExt& operator=(InterningExt &&) = delete;
                
                virtual ~InterningExt() = default;
                
                //reader
                
                //writer
                
                //virtual init
                void init(rd::Lifetime lifetime) const override;
                
                //identify
                void identify(const rd::Identities &identities, rd::RdId const &id) const override;
                
                //getters
                rd::RdProperty<InterningExtRootModel, rd::Polymorphic<InterningExtRootModel>> const & get_root() const;
                
                //intern
                
                //equals trait
                private:
                
                //equality operators
                public:
                friend bool operator==(const InterningExt &lhs, const InterningExt &rhs);
                friend bool operator!=(const InterningExt &lhs, const InterningExt &rhs);
                
                //hash code trait
                
                //type name trait
                
                //static type name trait
            };
        };
    };
};

#pragma warning( pop )


//hash code trait

#endif // InterningExt_H

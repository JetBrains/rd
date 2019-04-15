#ifndef InterningExtRootModel_H
#define InterningExtRootModel_H

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
            class InterningExtRootModel : public rd::IPolymorphicSerializable, public rd::RdBindableBase
            {
                
                //companion
                
                //custom serializers
                private:
                using __StringInternedAtInternScopeInExtSerializer = rd::InternedSerializer<rd::Polymorphic<std::wstring>, rd::util::getPlatformIndependentHash("InternScopeInExt")>;
                using __StringInternedAtInternScopeOutOfExtSerializer = rd::InternedSerializer<rd::Polymorphic<std::wstring>, rd::util::getPlatformIndependentHash("InternScopeOutOfExt")>;
                using __StringInternedAtProtocolSerializer = rd::InternedSerializer<rd::Polymorphic<std::wstring>, rd::util::getPlatformIndependentHash("Protocol")>;
                
                //fields
                protected:
                rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtInternScopeInExtSerializer> internedLocally_;
                rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtInternScopeOutOfExtSerializer> internedExternally_;
                rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtProtocolSerializer> internedInProtocol_;
                
                mutable optional<rd::SerializationCtx> mySerializationContext;
                
                //initializer
                private:
                void initialize();
                
                //primary ctor
                public:
                InterningExtRootModel(rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtInternScopeInExtSerializer> internedLocally_, rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtInternScopeOutOfExtSerializer> internedExternally_, rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtProtocolSerializer> internedInProtocol_);
                
                //secondary constructor
                
                //default ctors and dtors
                
                InterningExtRootModel();
                
                InterningExtRootModel(InterningExtRootModel &&) = default;
                
                InterningExtRootModel& operator=(InterningExtRootModel &&) = default;
                
                virtual ~InterningExtRootModel() = default;
                
                //reader
                static InterningExtRootModel read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer);
                
                //writer
                void write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const override;
                
                //virtual init
                void init(rd::Lifetime lifetime) const override;
                
                //identify
                void identify(const rd::Identities &identities, rd::RdId const &id) const override;
                
                //getters
                rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtInternScopeInExtSerializer> const & get_internedLocally() const;
                rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtInternScopeOutOfExtSerializer> const & get_internedExternally() const;
                rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtProtocolSerializer> const & get_internedInProtocol() const;
                
                //intern
                const rd::SerializationCtx & get_serialization_context() const override;
                
                //equals trait
                private:
                bool equals(rd::ISerializable const& object) const override;
                
                //equality operators
                public:
                friend bool operator==(const InterningExtRootModel &lhs, const InterningExtRootModel &rhs);
                friend bool operator!=(const InterningExtRootModel &lhs, const InterningExtRootModel &rhs);
                
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

#endif // InterningExtRootModel_H

#ifndef InterningTestModel_H
#define InterningTestModel_H

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

#include "WrappedStringModel.h"


#pragma warning( push )
#pragma warning( disable:4250 )
#pragma warning( disable:4307 )
namespace rd {
    namespace test {
        namespace util {
            class InterningTestModel : public rd::IPolymorphicSerializable, public rd::RdBindableBase
            {
                
                //companion
                
                //custom serializers
                private:
                
                //fields
                protected:
                rd::Wrapper<std::wstring> searchLabel_;
                rd::RdMap<int32_t, WrappedStringModel, rd::Polymorphic<int32_t>, rd::Polymorphic<WrappedStringModel>> issues_;
                
                mutable optional<rd::SerializationCtx> mySerializationContext;
                
                //initializer
                private:
                void initialize();
                
                //primary ctor
                public:
                InterningTestModel(rd::Wrapper<std::wstring> searchLabel_, rd::RdMap<int32_t, WrappedStringModel, rd::Polymorphic<int32_t>, rd::Polymorphic<WrappedStringModel>> issues_);
                
                //secondary constructor
                InterningTestModel(rd::Wrapper<std::wstring> searchLabel_);
                
                //default ctors and dtors
                
                InterningTestModel() = delete;
                
                InterningTestModel(InterningTestModel &&) = default;
                
                InterningTestModel& operator=(InterningTestModel &&) = default;
                
                virtual ~InterningTestModel() = default;
                
                //reader
                static InterningTestModel read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer);
                
                //writer
                void write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const override;
                
                //virtual init
                void init(rd::Lifetime lifetime) const override;
                
                //identify
                void identify(const rd::Identities &identities, rd::RdId const &id) const override;
                
                //getters
                std::wstring const & get_searchLabel() const;
                rd::RdMap<int32_t, WrappedStringModel, rd::Polymorphic<int32_t>, rd::Polymorphic<WrappedStringModel>> const & get_issues() const;
                
                //intern
                const rd::SerializationCtx & get_serialization_context() const override;
                
                //equals trait
                private:
                bool equals(rd::ISerializable const& object) const override;
                
                //equality operators
                public:
                friend bool operator==(const InterningTestModel &lhs, const InterningTestModel &rhs);
                friend bool operator!=(const InterningTestModel &lhs, const InterningTestModel &rhs);
                
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

#endif // InterningTestModel_H

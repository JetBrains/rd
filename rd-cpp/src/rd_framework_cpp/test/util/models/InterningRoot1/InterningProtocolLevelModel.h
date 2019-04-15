#ifndef InterningProtocolLevelModel_H
#define InterningProtocolLevelModel_H

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

#include "ProtocolWrappedStringModel.h"


#pragma warning( push )
#pragma warning( disable:4250 )
#pragma warning( disable:4307 )
namespace rd {
    namespace test {
        namespace util {
            class InterningProtocolLevelModel : public rd::IPolymorphicSerializable, public rd::RdBindableBase
            {
                
                //companion
                
                //custom serializers
                private:
                
                //fields
                protected:
                rd::Wrapper<std::wstring> searchLabel_;
                rd::RdMap<int32_t, ProtocolWrappedStringModel, rd::Polymorphic<int32_t>, rd::Polymorphic<ProtocolWrappedStringModel>> issues_;
                
                
                //initializer
                private:
                void initialize();
                
                //primary ctor
                public:
                InterningProtocolLevelModel(rd::Wrapper<std::wstring> searchLabel_, rd::RdMap<int32_t, ProtocolWrappedStringModel, rd::Polymorphic<int32_t>, rd::Polymorphic<ProtocolWrappedStringModel>> issues_);
                
                //secondary constructor
                InterningProtocolLevelModel(rd::Wrapper<std::wstring> searchLabel_);
                
                //default ctors and dtors
                
                InterningProtocolLevelModel() = delete;
                
                InterningProtocolLevelModel(InterningProtocolLevelModel &&) = default;
                
                InterningProtocolLevelModel& operator=(InterningProtocolLevelModel &&) = default;
                
                virtual ~InterningProtocolLevelModel() = default;
                
                //reader
                static InterningProtocolLevelModel read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer);
                
                //writer
                void write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const override;
                
                //virtual init
                void init(rd::Lifetime lifetime) const override;
                
                //identify
                void identify(const rd::Identities &identities, rd::RdId const &id) const override;
                
                //getters
                std::wstring const & get_searchLabel() const;
                rd::RdMap<int32_t, ProtocolWrappedStringModel, rd::Polymorphic<int32_t>, rd::Polymorphic<ProtocolWrappedStringModel>> const & get_issues() const;
                
                //intern
                
                //equals trait
                private:
                bool equals(rd::ISerializable const& object) const override;
                
                //equality operators
                public:
                friend bool operator==(const InterningProtocolLevelModel &lhs, const InterningProtocolLevelModel &rhs);
                friend bool operator!=(const InterningProtocolLevelModel &lhs, const InterningProtocolLevelModel &rhs);
                
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

#endif // InterningProtocolLevelModel_H

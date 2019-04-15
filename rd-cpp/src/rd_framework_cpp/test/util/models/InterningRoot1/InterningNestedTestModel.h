#ifndef InterningNestedTestModel_H
#define InterningNestedTestModel_H

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

#include "InterningNestedTestModel.h"


#pragma warning( push )
#pragma warning( disable:4250 )
#pragma warning( disable:4307 )
namespace rd {
    namespace test {
        namespace util {
            
            //data
            class InterningNestedTestModel : public rd::IPolymorphicSerializable
            {
                
                //companion
                
                //custom serializers
                private:
                
                //fields
                protected:
                rd::Wrapper<std::wstring> value_;
                rd::Wrapper<InterningNestedTestModel> inner_;
                
                
                //initializer
                private:
                void initialize();
                
                //primary ctor
                public:
                InterningNestedTestModel(rd::Wrapper<std::wstring> value_, rd::Wrapper<InterningNestedTestModel> inner_);
                
                //secondary constructor
                
                //default ctors and dtors
                
                InterningNestedTestModel() = delete;
                
                InterningNestedTestModel(InterningNestedTestModel const &) = default;
                
                InterningNestedTestModel& operator=(InterningNestedTestModel const &) = default;
                
                InterningNestedTestModel(InterningNestedTestModel &&) = default;
                
                InterningNestedTestModel& operator=(InterningNestedTestModel &&) = default;
                
                virtual ~InterningNestedTestModel() = default;
                
                //reader
                static InterningNestedTestModel read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer);
                
                //writer
                void write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const override;
                
                //virtual init
                
                //identify
                
                //getters
                std::wstring const & get_value() const;
                rd::Wrapper<InterningNestedTestModel> const & get_inner() const;
                
                //intern
                
                //equals trait
                private:
                bool equals(rd::ISerializable const& object) const override;
                
                //equality operators
                public:
                friend bool operator==(const InterningNestedTestModel &lhs, const InterningNestedTestModel &rhs);
                friend bool operator!=(const InterningNestedTestModel &lhs, const InterningNestedTestModel &rhs);
                
                //hash code trait
                size_t hashCode() const override;
                
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
namespace std {
    template <> struct hash<rd::test::util::InterningNestedTestModel> {
        size_t operator()(const rd::test::util::InterningNestedTestModel & value) const {
            return value.hashCode();
        }
    };
}

#endif // InterningNestedTestModel_H

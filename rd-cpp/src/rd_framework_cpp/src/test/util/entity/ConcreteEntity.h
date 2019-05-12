#ifndef ConcreteEntity_H
#define ConcreteEntity_H

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

#include "AbstractEntity.h"


#pragma warning( push )
#pragma warning( disable:4250 )
#pragma warning( disable:4307 )
namespace rd {
	namespace test {
		namespace util {
			class ConcreteEntity : public AbstractEntity {

				//companion

				//custom serializers
			private:

				//fields
			protected:
				rd::Wrapper<std::wstring> stringValue_;


				//initializer
			private:
				void initialize();

				//primary ctor
			public:
				ConcreteEntity(rd::Wrapper<std::wstring> stringValue_, rd::Wrapper<std::wstring> name_);

				//secondary constructor

				//default ctors and dtors

				ConcreteEntity() = delete;

				ConcreteEntity(ConcreteEntity const &) = default;

				ConcreteEntity &operator=(ConcreteEntity const &) = default;

				ConcreteEntity(ConcreteEntity &&) = default;

				ConcreteEntity &operator=(ConcreteEntity &&) = default;

				virtual ~ConcreteEntity() = default;

				//reader
				static ConcreteEntity read(rd::SerializationCtx const &ctx, rd::Buffer const &buffer);

				//writer
				void write(rd::SerializationCtx const &ctx, rd::Buffer const &buffer) const override;

				//virtual init

				//identify

				//getters
				std::wstring const &get_stringValue() const;

				//intern

				//equals trait
			private:
				bool equals(rd::ISerializable const &object) const override;

				//equality operators
			public:
				friend bool operator==(const ConcreteEntity &lhs, const ConcreteEntity &rhs);

				friend bool operator!=(const ConcreteEntity &lhs, const ConcreteEntity &rhs);

				//hash code trait
				size_t hashCode() const override;

				//type name trait
				std::string type_name() const override;

				//static type name trait
				static std::string static_type_name();

				friend std::string to_string(ConcreteEntity const &value);
			};
		};
	}
}

#pragma warning( pop )


//hash code trait
namespace std {
    template <> struct hash<rd::test::util::ConcreteEntity> {
        size_t operator()(const rd::test::util::ConcreteEntity & value) const {
            return value.hashCode();
        }
    };
}

#endif // ConcreteEntity_H

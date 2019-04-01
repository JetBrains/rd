#ifndef FakeEntity_H
#define FakeEntity_H

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
			class FakeEntity : public AbstractEntity {

				//companion

				//custom serializers
			private:

				//fields
			protected:
				bool booleanValue_;


				//initializer
			private:
				void initialize();

				//primary ctor
			public:
				FakeEntity(bool booleanValue_, rd::Wrapper<std::wstring> name_);

				//secondary constructor

				//default ctors and dtors

				FakeEntity() = delete;

				FakeEntity(FakeEntity const &) = default;

				FakeEntity &operator=(FakeEntity const &) = default;

				FakeEntity(FakeEntity &&) = default;

				FakeEntity &operator=(FakeEntity &&) = default;

				virtual ~FakeEntity() = default;

				//reader
				static FakeEntity read(rd::SerializationCtx const &ctx, rd::Buffer const &buffer);

				//writer
				void write(rd::SerializationCtx const &ctx, rd::Buffer const &buffer) const override;

				//virtual init

				//identify

				//getters
				bool const &get_booleanValue() const;

				//intern

				//equals trait
			private:
				bool equals(rd::ISerializable const &object) const override;

				//equality operators
			public:
				friend bool operator==(const FakeEntity &lhs, const FakeEntity &rhs);

				friend bool operator!=(const FakeEntity &lhs, const FakeEntity &rhs);

				//hash code trait
				size_t hashCode() const override;

				//type name trait
				std::string type_name() const override;

				//static type name trait
				static std::string static_type_name();
			};
		};
	}
}

#pragma warning( pop )


//hash code trait
namespace std {
    template <> struct hash<rd::test::util::FakeEntity> {
        size_t operator()(const rd::test::util::FakeEntity & value) const {
            return value.hashCode();
        }
    };
}

#endif // FakeEntity_H

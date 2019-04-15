#ifndef AbstractEntity_Unknown_H
#define AbstractEntity_Unknown_H

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
			class AbstractEntity_Unknown : public AbstractEntity, public rd::IUnknownInstance {

				//companion
				friend class AbstractEntity;

				//custom serializers
			private:

				//fields
			protected:
				rd::RdId unknownId_;
				rd::Buffer::ByteArray unknownBytes_;


				//initializer
			private:
				void initialize();

				//primary ctor
			public:
				AbstractEntity_Unknown(rd::Wrapper<std::wstring> name_, rd::RdId unknownId_,
									 rd::Buffer::ByteArray unknownBytes_);

				//secondary constructor

				//default ctors and dtors

				AbstractEntity_Unknown() = delete;

				AbstractEntity_Unknown(AbstractEntity_Unknown const &) = default;

				AbstractEntity_Unknown &operator=(AbstractEntity_Unknown const &) = default;

				AbstractEntity_Unknown(AbstractEntity_Unknown &&) = default;

				AbstractEntity_Unknown &operator=(AbstractEntity_Unknown &&) = default;

				virtual ~AbstractEntity_Unknown() = default;

				//reader
				static AbstractEntity_Unknown read(rd::SerializationCtx const &ctx, rd::Buffer const &buffer);

				//writer
				void write(rd::SerializationCtx const &ctx, rd::Buffer const &buffer) const override;

				//virtual init

				//identify

				//getters

				//intern

				//equals trait
			private:
				bool equals(rd::ISerializable const &object) const override;

				//equality operators
			public:
				friend bool operator==(const AbstractEntity_Unknown &lhs, const AbstractEntity_Unknown &rhs);

				friend bool operator!=(const AbstractEntity_Unknown &lhs, const AbstractEntity_Unknown &rhs);

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
    template <> struct hash<rd::test::util::AbstractEntity_Unknown> {
        size_t operator()(const rd::test::util::AbstractEntity_Unknown & value) const {
            return value.hashCode();
        }
    };
}

#endif // AbstractEntity_Unknown_H

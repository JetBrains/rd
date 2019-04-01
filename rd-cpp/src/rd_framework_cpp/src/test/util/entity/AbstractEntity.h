#ifndef AbstractEntity_H
#define AbstractEntity_H

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

			//abstract
			class AbstractEntity : public rd::IPolymorphicSerializable {

				//companion

				//custom serializers
			private:

				//fields
			protected:
				rd::Wrapper<std::wstring> name_;


				//initializer
			private:
				void initialize();

				//primary ctor
			public:
				AbstractEntity(rd::Wrapper<std::wstring> name_);

				//secondary constructor

				//default ctors and dtors

				AbstractEntity() = delete;

				AbstractEntity(AbstractEntity const &) = default;

				AbstractEntity &operator=(AbstractEntity const &) = default;

				AbstractEntity(AbstractEntity &&) = default;

				AbstractEntity &operator=(AbstractEntity &&) = default;

				virtual ~AbstractEntity() = default;

				//reader
				static rd::Wrapper<AbstractEntity>
				readUnknownInstance(rd::SerializationCtx const &ctx, rd::Buffer const &buffer,
									rd::RdId const &unknownId, int32_t size);

				//writer
				virtual void write(rd::SerializationCtx const &ctx, rd::Buffer const &buffer) const override = 0;

				//virtual init

				//identify

				//getters
				std::wstring const &get_name() const;

				//intern

				//equals trait
			private:

				//equality operators
			public:
				friend bool operator==(const AbstractEntity &lhs, const AbstractEntity &rhs);

				friend bool operator!=(const AbstractEntity &lhs, const AbstractEntity &rhs);

				//hash code trait
				virtual size_t hashCode() const override = 0;

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
    template <> struct hash<rd::test::util::AbstractEntity> {
        size_t operator()(const rd::test::util::AbstractEntity & value) const {
            return value.hashCode();
        }
    };
}

#endif // AbstractEntity_H

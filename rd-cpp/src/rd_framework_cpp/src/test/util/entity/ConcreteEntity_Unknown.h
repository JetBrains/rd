#ifndef ConcreteEntity_Unknown_H
#define ConcreteEntity_Unknown_H

#include "Buffer.h"
#include "RdId.h"
#include "RdMap.h"
#include "RdSignal.h"
#include "ISerializable.h"
#include "SerializationCtx.h"
#include "IUnknownInstance.h"
#include "RdCall.h"

#include <vector>
#include <type_traits>

#include "AbstractEntity.h"


#pragma warning( push )
#pragma warning( disable:4250 )
namespace rd {
	namespace test {
		namespace util {
			class ConcreteEntity_Unknown : public AbstractEntity, public IUnknownInstance {

				//companion

				//custom serializers
			private:

				//fields
			protected:
				RdId unknownId;
				Buffer::ByteArray unknownBytes;


				//initializer
			private:
				void initialize();

				//primary ctor
			public:
				explicit ConcreteEntity_Unknown(std::wstring filePath, RdId unknownId, Buffer::ByteArray unknownBytes);

				//default ctors and dtors

				ConcreteEntity_Unknown() {
					initialize();
				};

				ConcreteEntity_Unknown(ConcreteEntity_Unknown const &) = default;

				ConcreteEntity_Unknown &operator=(ConcreteEntity_Unknown const &) = default;

				ConcreteEntity_Unknown(ConcreteEntity_Unknown &&) = default;

				ConcreteEntity_Unknown &operator=(ConcreteEntity_Unknown &&) = default;

				virtual ~ConcreteEntity_Unknown() = default;

				//reader
				static ConcreteEntity_Unknown read(SerializationCtx const &ctx, Buffer const &buffer);

				//writer
				void write(SerializationCtx const &ctx, Buffer const &buffer) const override;

				//virtual init

				//identify

				//getters

				//equals trait
				bool equals(ISerializable const &object) const override;

				//equality operators
				friend bool operator==(const ConcreteEntity_Unknown &lhs, const ConcreteEntity_Unknown &rhs);

				friend bool operator!=(const ConcreteEntity_Unknown &lhs, const ConcreteEntity_Unknown &rhs);

				//hash code trait
				size_t hashCode() const override;
			};
		}
	}
}

#pragma warning( pop )


//hash code trait
namespace std {
	template<>
	struct hash<rd::test::util::ConcreteEntity_Unknown> {
		size_t operator()(const rd::test::util::ConcreteEntity_Unknown &value) const {
			return value.hashCode();
		}
	};
}

#endif // ConcreteEntity_Unknown_H

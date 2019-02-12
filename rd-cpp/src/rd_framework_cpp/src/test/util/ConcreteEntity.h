#ifndef ConcreteEntity_H
#define ConcreteEntity_H

#include "Buffer.h"
#include "RdId.h"
#include "RdSignal.h"
#include "ISerializable.h"
#include "SerializationCtx.h"
#include "RdCall.h"

#include <vector>
#include <string>
#include <type_traits>

#include "AbstractEntity.h"


#pragma warning( push )
#pragma warning( disable:4250 )
namespace rd {
	namespace test {
		namespace util {
			class ConcreteEntity : public AbstractEntity {

				//companion

				//custom serializers
			private:

				//fields
			protected:

				//initializer
			private:
				void initialize();

				//primary ctor
			public:
				explicit ConcreteEntity(::std::wstring filePath);

				//default ctors and dtors

				ConcreteEntity() {
					initialize();
				};

				ConcreteEntity(ConcreteEntity const &) = default;

				ConcreteEntity &operator=(ConcreteEntity const &) = default;

				ConcreteEntity(ConcreteEntity &&) = default;

				ConcreteEntity &operator=(ConcreteEntity &&) = default;

				virtual ~ConcreteEntity() = default;

				//reader
				static ConcreteEntity read(SerializationCtx const &ctx, Buffer const &buffer);

				//writer
				void write(SerializationCtx const &ctx, Buffer const &buffer) const override;

				//virtual init

				//identify

				//getters

				//equals trait
				bool equals(ISerializable const &object) const override;

				//equality operators
				friend bool operator==(const ConcreteEntity &lhs, const ConcreteEntity &rhs);

				friend bool operator!=(const ConcreteEntity &lhs, const ConcreteEntity &rhs);

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
	struct hash<rd::test::util::ConcreteEntity> {
		size_t operator()(const rd::test::util::ConcreteEntity &value) const {
			return value.hashCode();
		}
	};
}

#endif // ConcreteEntity_H

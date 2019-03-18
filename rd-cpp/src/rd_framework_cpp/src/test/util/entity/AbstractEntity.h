#ifndef AbstractEntity_H
#define AbstractEntity_H

#include "Buffer.h"
#include "RdId.h"
#include "RdSignal.h"
#include "ISerializable.h"
#include "SerializationCtx.h"

#include <cstdint>
#include <vector>


#pragma warning( push )
#pragma warning( disable:4250 )

namespace rd {
	namespace test {
		namespace util {
			//abstract
			class AbstractEntity : public IPolymorphicSerializable {
				//fields
			protected:
				std::wstring filePath;


				//initializer
			private:
				void initialize();

				//primary ctor
			public:
				explicit AbstractEntity(std::wstring filePath);

				//default ctors and dtors

				AbstractEntity() {
					initialize();
				};

				AbstractEntity(AbstractEntity const &) = default;

				AbstractEntity &operator=(AbstractEntity const &) = default;

				AbstractEntity(AbstractEntity &&) = default;

				AbstractEntity &operator=(AbstractEntity &&) = default;

				virtual ~AbstractEntity() = default;

				//reader
				static Wrapper<AbstractEntity>
				readUnknownInstance(SerializationCtx const &ctx, Buffer const &buffer, RdId const &unknownId,
									int32_t size);

				//writer
				void write(SerializationCtx const &ctx, Buffer const &buffer) const override = 0;

				//virtual init

				//identify

				//getters
				std::wstring const &get_filePath() const;

				//equals trait
				bool equals(ISerializable const &object) const override;

				//equality operators
				friend bool operator==(const AbstractEntity &lhs, const AbstractEntity &rhs);

				friend bool operator!=(const AbstractEntity &lhs, const AbstractEntity &rhs);

				//hash code trait
				size_t hashCode() const override;

				std::string type_name() const override;
			};

		}
	}
}

#pragma warning( pop )

namespace std {
	template<>
	struct hash<rd::test::util::AbstractEntity> {
		size_t operator()(const rd::test::util::AbstractEntity &value) const {
			return value.hashCode();
		}
	};
}

#endif // AbstractEntity_H

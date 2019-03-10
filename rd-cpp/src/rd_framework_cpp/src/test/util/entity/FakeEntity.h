//
// Created by jetbrains on 23.02.2019.
//

#ifndef RD_CPP_FAKEENTITY_H
#define RD_CPP_FAKEENTITY_H

#include "AbstractEntity.h"

namespace rd {
	namespace test {
		namespace util {
			class FakeEntity : public AbstractEntity {

			public:
				FakeEntity();

				explicit FakeEntity(std::wstring filePath);

				static FakeEntity read(SerializationCtx const &, Buffer const &);

				void write(SerializationCtx const &ctx, Buffer const &buffer) const override;

				bool equals(ISerializable const &object) const override;

				size_t hashCode() const override;

				virtual bool equals(IPolymorphicSerializable const &serializable) const;

				std::string type_name() const override;
			};
		}
	}
}


#endif //RD_CPP_FAKEENTITY_H

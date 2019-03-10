//
// Created by jetbrains on 23.02.2019.
//

#include "FakeEntity.h"

namespace rd {
	namespace test {
		namespace util {
			FakeEntity::FakeEntity() {}

			FakeEntity::FakeEntity(std::wstring filePath) : AbstractEntity(
					std::move(filePath)) {}

			FakeEntity FakeEntity::read(SerializationCtx const&, Buffer const&) {
				return FakeEntity(L"");
			}

			void FakeEntity::write(const rd::SerializationCtx &ctx, const rd::Buffer &buffer) const {

			}

			bool FakeEntity::equals(const rd::ISerializable &object) const {
				return false;
			}

			size_t FakeEntity::hashCode() const {
				return 0;
			}

			std::string FakeEntity::type_name() const {
				return "FakeEntity";
			}

			bool FakeEntity::equals(IPolymorphicSerializable const &serializable) const {
				return false;
			}
		}
	}
}
#include "ConcreteEntity_Unknown.h"


namespace rd {
	namespace test {
		namespace util {
			//companion

			//initializer
			void ConcreteEntity_Unknown::initialize() {
			}

			//primary ctor
			ConcreteEntity_Unknown::ConcreteEntity_Unknown(std::wstring filePath, RdId unknownId,
														   Buffer::ByteArray unknownBytes) : AbstractEntity(
					std::move(filePath)), IUnknownInstance(std::move(unknownId)), unknownBytes(
					std::move(unknownBytes)) { initialize(); }

			//reader
			ConcreteEntity_Unknown ConcreteEntity_Unknown::read(SerializationCtx const &ctx, Buffer const &buffer) {
				throw std::logic_error("Unknown instances should not be read via serializer");
			}

			//writer
			void ConcreteEntity_Unknown::write(SerializationCtx const &ctx, Buffer const &buffer) const {
				buffer.writeWString(filePath);
				buffer.writeByteArrayRaw(unknownBytes);
			}

			//virtual init

			//identify

			//getters

			//equals trait
			bool ConcreteEntity_Unknown::equals(ISerializable const &object) const {
				auto const &other = dynamic_cast<ConcreteEntity_Unknown const &>(object);
				if (this == &other) return true;
				if (this->filePath != other.filePath) return false;

				return true;
			}

			//equality operators
			bool operator==(const ConcreteEntity_Unknown &lhs, const ConcreteEntity_Unknown &rhs) {
				if (typeid(lhs) != typeid(rhs)) return false;
				return lhs.equals(rhs);
			}

			bool operator!=(const ConcreteEntity_Unknown &lhs, const ConcreteEntity_Unknown &rhs) {
				return !(lhs == rhs);
			}

			//hash code trait
			size_t ConcreteEntity_Unknown::hashCode() const {
				size_t __r = 0;
				__r = __r * 31 + (std::hash<std::wstring>()(get_filePath()));
				return __r;
			}

		}
	}
}

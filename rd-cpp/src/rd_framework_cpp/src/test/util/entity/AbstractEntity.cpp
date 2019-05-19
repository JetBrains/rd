#include "AbstractEntity.h"


#include "AbstractEntity_Unknown.h"

namespace rd {
	namespace test {
		namespace util {

			//companion

			//initializer
			void AbstractEntity::initialize() {
			}

			//primary ctor
			AbstractEntity::AbstractEntity(rd::Wrapper<std::wstring> name_) :
					rd::IPolymorphicSerializable(), name_(std::move(name_)) {
				initialize();
			}

			//secondary constructor

			//reader
			rd::Wrapper<AbstractEntity>
			AbstractEntity::readUnknownInstance(rd::SerializationCtx const &ctx, rd::Buffer const &buffer,
											  rd::RdId const &unknownId, int32_t size) {
				int32_t objectStartPosition = buffer.get_position();
				auto name_ = buffer.read_wstring();
				auto unknownBytes = rd::Buffer::ByteArray(objectStartPosition + size - buffer.get_position());
				buffer.read_byte_array_raw(unknownBytes);
				AbstractEntity_Unknown res{std::move(name_), unknownId, unknownBytes};
				return Wrapper<AbstractEntity_Unknown>(std::move(res));
			}

			//writer

			//virtual init

			//identify

			//getters
			std::wstring const &AbstractEntity::get_name() const {
				return *name_;
			}

			//intern

			//equals trait

			//equality operators
			bool operator==(const AbstractEntity &lhs, const AbstractEntity &rhs) {
				if (lhs.type_name() != rhs.type_name()) return false;
				return lhs.equals(rhs);
			}

			bool operator!=(const AbstractEntity &lhs, const AbstractEntity &rhs) {
				return !(lhs == rhs);
			}

			//hash code trait
			size_t AbstractEntity::hashCode() const noexcept {
				size_t __r = 0;
				__r = __r * 31 + (std::hash<std::wstring>()(get_name()));
				return __r;
			}

			std::string AbstractEntity::type_name() const {
				return "AbstractEntity";
			}

			//static type name trait
			std::string AbstractEntity::static_type_name()
			{
				return "AbstractEntity";
			}

			std::string to_string(AbstractEntity const &value) {
				std::string res = "AbstractEntity\n";
				res += "\tname_ = " + rd::to_string(value.name_) + '\n';
				return res;
			}
		};
	}
}

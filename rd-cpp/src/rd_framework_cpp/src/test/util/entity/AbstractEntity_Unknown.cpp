#include "AbstractEntity_Unknown.h"


namespace rd {
	namespace test {
		namespace util {

			//companion

			//initializer
			void AbstractEntity_Unknown::initialize() {
			}

			//primary ctor
			AbstractEntity_Unknown::AbstractEntity_Unknown(rd::Wrapper<std::wstring> name_, rd::RdId unknownId_,
													   rd::Buffer::ByteArray unknownBytes_) :
					AbstractEntity(std::move(name_)), rd::IUnknownInstance(std::move(unknownId_)) {
				initialize();
			}

			//secondary constructor

			//reader
			AbstractEntity_Unknown AbstractEntity_Unknown::read(rd::SerializationCtx  &ctx, rd::Buffer &buffer) {
				throw std::logic_error("Unknown instances should not be read via serializer");
			}

			//writer
			void AbstractEntity_Unknown::write(rd::SerializationCtx  &ctx, rd::Buffer &buffer) const {
				buffer.write_wstring(name_);
				buffer.write_byte_array_raw(unknownBytes_);
			}

			//virtual init

			//identify

			//getters

			//intern

			//equals trait
			bool AbstractEntity_Unknown::equals(rd::ISerializable const &object) const {
				auto const &other = dynamic_cast<AbstractEntity_Unknown const &>(object);
				if (this == &other) return true;
				if (this->name_ != other.name_) return false;

				return true;
			}

			//equality operators
			bool operator==(const AbstractEntity_Unknown &lhs, const AbstractEntity_Unknown &rhs) {
				if (lhs.type_name() != rhs.type_name()) return false;
				return lhs.equals(rhs);
			}

			bool operator!=(const AbstractEntity_Unknown &lhs, const AbstractEntity_Unknown &rhs) {
				return !(lhs == rhs);
			}

			//hash code trait
			size_t AbstractEntity_Unknown::hashCode() const noexcept {
				size_t __r = 0;
				__r = __r * 31 + (std::hash<std::wstring>()(get_name()));
				return __r;
			}

			std::string AbstractEntity_Unknown::type_name() const {
				return "AbstractEntity_Unknown";
			}

			//static type name trait
			std::string AbstractEntity_Unknown::static_type_name()
			{
				return "AbstractEntity_Unknown";
			}
		};
	}
}

#include "ConcreteEntity.h"


namespace rd {
	namespace test {
		namespace util {

			//companion

			//initializer
			void ConcreteEntity::initialize() {
			}

			//primary ctor
			ConcreteEntity::ConcreteEntity(rd::Wrapper<std::wstring> stringValue_,
												   rd::Wrapper<std::wstring> name_) :
					AbstractEntity(std::move(name_)), stringValue_(std::move(stringValue_)) {
				initialize();
			}

			//secondary constructor

			//reader
			ConcreteEntity ConcreteEntity::read(rd::SerializationCtx  &ctx, rd::Buffer &buffer) {
				auto name_ = buffer.read_wstring();
				auto stringValue_ = buffer.read_wstring();
				ConcreteEntity res{std::move(stringValue_), std::move(name_)};
				return res;
			}

			//writer
			void ConcreteEntity::write(rd::SerializationCtx  &ctx, rd::Buffer &buffer) const {
				buffer.write_wstring(name_);
				buffer.write_wstring(stringValue_);
			}

			//virtual init

			//identify

			//getters
			std::wstring const &ConcreteEntity::get_stringValue() const {
				return *stringValue_;
			}

			//intern

			//equals trait
			bool ConcreteEntity::equals(rd::ISerializable const &object) const {
				auto const &other = dynamic_cast<ConcreteEntity const &>(object);
				if (this == &other) return true;
				if (this->stringValue_ != other.stringValue_) return false;
				if (this->name_ != other.name_) return false;

				return true;
			}

			//equality operators
			bool operator==(const ConcreteEntity &lhs, const ConcreteEntity &rhs) {
				if (lhs.type_name() != rhs.type_name()) return false;
				return lhs.equals(rhs);
			}

			bool operator!=(const ConcreteEntity &lhs, const ConcreteEntity &rhs) {
				return !(lhs == rhs);
			}

			//hash code trait
			size_t ConcreteEntity::hashCode() const noexcept {
				size_t __r = 0;
				__r = __r * 31 + (std::hash<std::wstring>()(get_stringValue()));
				__r = __r * 31 + (std::hash<std::wstring>()(get_name()));
				return __r;
			}

			std::string ConcreteEntity::type_name() const {
				return "ConcreteEntity";
			}

			//static type name trait
			std::string ConcreteEntity::static_type_name()
			{
				return "ConcreteEntity";
			}

			std::string to_string(ConcreteEntity const &value) {
				std::string res = "ConcreteEntity\n";
				res += "\tname_ = " + rd::to_string(value.name_) + '\n';
				res += "\tstringValue_ = " + rd::to_string(value.stringValue_) + '\n';
				return res;
			}
		};
	}
}

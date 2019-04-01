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
			ConcreteEntity ConcreteEntity::read(rd::SerializationCtx const &ctx, rd::Buffer const &buffer) {
				auto name_ = buffer.readWString();
				auto stringValue_ = buffer.readWString();
				ConcreteEntity res{std::move(stringValue_), std::move(name_)};
				return res;
			}

			//writer
			void ConcreteEntity::write(rd::SerializationCtx const &ctx, rd::Buffer const &buffer) const {
				buffer.writeWString(name_);
				buffer.writeWString(stringValue_);
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
			size_t ConcreteEntity::hashCode() const {
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
		};
	}
}

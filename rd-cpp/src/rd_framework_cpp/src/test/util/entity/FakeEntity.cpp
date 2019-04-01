#include "FakeEntity.h"

namespace rd {
	namespace test {
		namespace util {
			//companion

			//initializer
			void FakeEntity::initialize() {
			}

			//primary ctor
			FakeEntity::FakeEntity(bool booleanValue_, rd::Wrapper<std::wstring> name_) :
					AbstractEntity(std::move(name_)), booleanValue_(std::move(booleanValue_)) {
				initialize();
			}

			//secondary constructor

			//reader
			FakeEntity FakeEntity::read(rd::SerializationCtx const &ctx, rd::Buffer const &buffer) {
				auto name_ = buffer.readWString();
				auto booleanValue_ = buffer.readBool();
				FakeEntity res{std::move(booleanValue_), std::move(name_)};
				return res;
			}

			//writer
			void FakeEntity::write(rd::SerializationCtx const &ctx, rd::Buffer const &buffer) const {
				buffer.writeWString(name_);
				buffer.writeBool(booleanValue_);
			}

			//virtual init

			//identify

			//getters
			bool const &FakeEntity::get_booleanValue() const {
				return booleanValue_;
			}

			//intern

			//equals trait
			bool FakeEntity::equals(rd::ISerializable const &object) const {
				auto const &other = dynamic_cast<FakeEntity const &>(object);
				if (this == &other) return true;
				if (this->booleanValue_ != other.booleanValue_) return false;
				if (this->name_ != other.name_) return false;

				return true;
			}

			//equality operators
			bool operator==(const FakeEntity &lhs, const FakeEntity &rhs) {
				if (lhs.type_name() != rhs.type_name()) return false;
				return lhs.equals(rhs);
			}

			bool operator!=(const FakeEntity &lhs, const FakeEntity &rhs) {
				return !(lhs == rhs);
			}

			//hash code trait
			size_t FakeEntity::hashCode() const {
				size_t __r = 0;
				__r = __r * 31 + (std::hash<bool>()(get_booleanValue()));
				__r = __r * 31 + (std::hash<std::wstring>()(get_name()));
				return __r;
			}

			std::string FakeEntity::type_name() const {
				return "FakeEntity";
			}

			//static type name trait
			std::string FakeEntity::static_type_name()
			{
				return "FakeEntity";
			}
		}
	}
}

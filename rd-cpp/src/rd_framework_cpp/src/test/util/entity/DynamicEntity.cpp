#include "DynamicEntity.h"

namespace rd {
	namespace test {
		namespace util {
			DynamicEntity DynamicEntity::read(SerializationCtx  &ctx, Buffer &buffer) {
				return DynamicEntity(RdProperty<int32_t>::read(ctx, buffer));
			}

			std::string DynamicEntity::type_name() const { return "DynamicEntity"; }

			void DynamicEntity::write(SerializationCtx  &ctx, Buffer &buffer) const {
				foo.write(ctx, buffer);
			}

			void DynamicEntity::create(IProtocol *protocol) {
				protocol->serializers->registry<DynamicEntity>();
			}

			void DynamicEntity::init(Lifetime lifetime) const {
				foo.bind(lifetime, this, "foo");
			}

			void DynamicEntity::identify(Identities const &identities, RdId const &id) const {
				foo.identify(identities, id.mix("foo"));
			}

			bool operator==(const DynamicEntity &lhs, const DynamicEntity &rhs) {
				return lhs.foo == rhs.foo;
			}

			bool operator!=(const DynamicEntity &lhs, const DynamicEntity &rhs) {
				return !(rhs == lhs);
			}

			size_t DynamicEntity::hashCode() const noexcept {
				return IPolymorphicSerializable::hashCode();
			}

			bool DynamicEntity::equals(const ISerializable &object) const {
				auto const &other = dynamic_cast<DynamicEntity const&>(object);
				return this == &object;
			}

			std::string DynamicEntity::static_type_name()
			{
				return "DynamicEntity";
			}

			std::string to_string(DynamicEntity const &value) {
				return to_string(value.foo);
			}
		}
	}
}

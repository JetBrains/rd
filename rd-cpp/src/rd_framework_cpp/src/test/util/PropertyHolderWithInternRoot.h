//
// Created by jetbrains on 3/11/2019.
//

#ifndef RD_CPP_PROPERTYHOLDERWITHINTERNROOT_H
#define RD_CPP_PROPERTYHOLDERWITHINTERNROOT_H


#include "RdProperty.h"
#include "SerializationCtx.h"

namespace rd {
	namespace test {
		namespace util {
			template<typename T, typename S = Polymorphic<T>>
			class PropertyHolderWithInternRoot : public RdBindableBase {
			public:
				RdProperty<T, S> property;
				tl::optional<SerializationCtx> mySerializationContext;

				explicit PropertyHolderWithInternRoot(RdProperty<T, S> property) : property(std::move(property)) {}

				void init(Lifetime lifetime) const override {
					property.bind(lifetime, this, "propertyHolderWithInternRoot");
					RdBindableBase::init(lifetime);
				}

				void identify(const Identities &identities, RdId const &id) const override {
					property.identify(identities, id.mix("propertyHolderWithInternRoot"));
					RdBindableBase::identify(identities, id);
				}

			private:
				const SerializationCtx &get_serialization_context() const override {
					return *mySerializationContext;
				}
			};
		}
	}
}


#endif //RD_CPP_PROPERTYHOLDERWITHINTERNROOT_H

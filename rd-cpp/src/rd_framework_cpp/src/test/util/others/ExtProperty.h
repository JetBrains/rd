#ifndef RD_CPP_EXTPROPERTY_H
#define RD_CPP_EXTPROPERTY_H


#include "ext/RdExtBase.h"
#include "impl/RdProperty.h"

namespace rd {
	namespace test {
		namespace util {
			template<typename T>
			class ExtProperty : public RdExtBase {
			public:
				RdProperty <T> property{T()};

				//region ctor/dtor

				explicit ExtProperty(T value) {
					property.set(std::move(value));
					property.slave();
				}

				ExtProperty(ExtProperty &&) = default;

				ExtProperty &operator=(ExtProperty &&) = default;

				virtual ~ExtProperty() = default;
				//endregion

				void bind(Lifetime lf, IRdDynamic const *parent, string_view name) const override {
					RdExtBase::bind(lf, parent, name);
					property.bind(lf, this, "property");
				}

				void identify(Identities const &identities, RdId const &id) const override {
					RdExtBase::identify(identities, id);
					property.identify(identities, id.mix("property"));
				}
			};
		}
	}
}


#endif //RD_CPP_EXTPROPERTY_H

//
// Created by jetbrains on 01.10.2018.
//

#include "DynamicExt.h"

namespace rd {
	namespace test {
		namespace util {
			DynamicExt::DynamicExt() {
				bar.slave();
			}

			DynamicExt::DynamicExt(RdProperty<std::wstring> bar, std::wstring debugName) : bar(
					std::move(bar)), debugName(std::move(debugName)) {}

			DynamicExt::DynamicExt(std::wstring const &bar, std::wstring const &debugName) : DynamicExt(
					RdProperty<std::wstring>(bar),
					debugName) {}

			void DynamicExt::init(Lifetime lifetime) const {
				RdExtBase::init(lifetime);
				bindPolymorphic(bar, lifetime, this, "bar");
			}

			void DynamicExt::identify(const IIdentities &identities, RdId const &id) const {
				RdBindableBase::identify(identities, id);
				identifyPolymorphic(bar, identities, id.mix(".bar"));
			}

			DynamicExt DynamicExt::read(SerializationCtx const &ctx, Buffer const &buffer) {
				throw std::invalid_argument("reading DynaimcExt is prohibited!");
			}

			void DynamicExt::write(SerializationCtx const &ctx, Buffer const &buffer) const {
				bar.write(ctx, buffer);
			}

			void DynamicExt::create(IProtocol *protocol) {
				protocol->serializers.registry<DynamicExt>();
			}

			/*
void DynamicExt::bind(Lifetime lf, IRdDynamic const *parent, std::wstring const &name) const {
    RdExtBase::bind(lf, parent, name);
    bar.bind(lf, this, "bar");
}

void DynamicExt::identify(IIdentities const &identities, RdId id) const {
    RdExtBase::identify(identities, id);
    bar.identify(identities, id.mix("bar"));
}*/


		}
	}
}

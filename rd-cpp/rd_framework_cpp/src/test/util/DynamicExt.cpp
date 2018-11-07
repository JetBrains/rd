//
// Created by jetbrains on 01.10.2018.
//

#include "DynamicExt.h"

DynamicExt::DynamicExt() {
    bindableChildren.emplace_back("bar", deleted_shared_ptr(bar));
    bar.slave();
}

DynamicExt::DynamicExt(RdProperty<std::string> bar, std::string debugName) : bar(
        std::move(bar)), debugName(std::move(debugName)) {}

DynamicExt::DynamicExt(std::string const &bar, std::string const &debugName) : DynamicExt(RdProperty<std::string>(bar),
                                                                                          debugName) {}

void DynamicExt::write(SerializationCtx const &ctx, Buffer const &buffer) const {
    bar.write(ctx, buffer);
}

void DynamicExt::registry(IProtocol *protocol) {
    protocol->serializers.registry<DynamicExt>([](SerializationCtx const &, Buffer const &) -> DynamicExt {
        throw std::invalid_argument("try to registry DynamicExt");
    });
}
/*
void DynamicExt::bind(Lifetime lf, IRdDynamic const *parent, std::string const &name) const {
    RdExtBase::bind(lf, parent, name);
    bar.bind(lf, this, "bar");
}

void DynamicExt::identify(IIdentities const &identities, RdId id) const {
    RdExtBase::identify(identities, id);
    bar.identify(identities, id.mix("bar"));
}*/


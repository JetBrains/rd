//
// Created by jetbrains on 13.08.2018.
//

#include "DynamicEntity.h"

DynamicEntity DynamicEntity::read(SerializationCtx const &ctx, Buffer const &buffer) {
    return DynamicEntity(RdProperty<int32_t>::read(ctx, buffer));
}

void DynamicEntity::write(SerializationCtx const &ctx, Buffer const &buffer) const {
    foo.write(ctx, buffer);
}

void DynamicEntity::create(IProtocol *protocol) {
    protocol->serializers.registry<DynamicEntity>();
}

void DynamicEntity::init(Lifetime lifetime) const {
    foo.bind(lifetime, this, "foo");
}

void DynamicEntity::identify(IIdentities const &identities, RdId id) const {
    foo.identify(identities, id.mix("foo"));
}

bool operator==(const DynamicEntity &lhs, const DynamicEntity &rhs) {
    return lhs.foo == rhs.foo;
}

bool operator!=(const DynamicEntity &lhs, const DynamicEntity &rhs) {
    return !(rhs == lhs);
}

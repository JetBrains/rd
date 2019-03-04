#include "SerializationCtx.h"

namespace rd {
//	SerializationCtx::SerializationCtx(const IProtocol &protocol) : serializers(protocol.serializers.get()) {}

//	SerializationCtx::SerializationCtx(const Serializers *const serializers) : serializers(serializers) {}

	SerializationCtx::SerializationCtx(const Serializers *serializers, roots_t intern_roots) :
			serializers(serializers), intern_roots(std::move(intern_roots)) {}

	SerializationCtx SerializationCtx::withInternRootsHere(RdBindableBase const &owner, std::initializer_list<std::string> new_roots) {
		roots_t next_roots = intern_roots;
		for (const auto &item : new_roots) {
			auto const &name = "InternRoot-" + item;
			InternRoot const &root = owner.getOrCreateExtension<InternRoot>(name);
			withId(root, owner.rdid.mix("." + name));
			intern_roots.emplace(getPlatformIndependentHash(item), &root);
		}
		return SerializationCtx(serializers, std::move(next_roots));
	}
}

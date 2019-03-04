#include "SerializationCtx.h"

namespace rd {
//	SerializationCtx::SerializationCtx(const IProtocol &protocol) : serializers(protocol.serializers.get()) {}

	SerializationCtx::SerializationCtx(const Serializers *const serializers) : serializers(serializers) {}

	SerializationCtx SerializationCtx::withInternRootsHere(RdBindableBase const &, std::string new_roots...) const {
		return SerializationCtx();
		//todo impl
	}

	/*SerializationCtx SerializationCtx::withInternRootHere(bool isMaster) const {
		return SerializationCtx(serializers, InternRoot(isMaster));
	}*/

	/*SerializationCtx::SerializationCtx(const Serializers *serializers, InternRoot internRoot) :
			serializers(serializers), internRoot(std::move(internRoot)) {}*/
}

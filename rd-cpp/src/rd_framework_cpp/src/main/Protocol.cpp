#include <utility>

#include "Protocol.h"

namespace rd {
	const Logger Protocol::initializationLogger;

	void Protocol::initialize() {
		internRoot = std::make_unique<InternRoot>();

		internRoot->rdid = RdId::Null().mix(InternRootName);
		scheduler->queue([this] {
			internRoot->bind(lifetime, this, InternRootName);
		});
	}

	Protocol::Protocol(std::shared_ptr<Identities> identity, IScheduler *scheduler, std::shared_ptr<IWire> wire, Lifetime lifetime) :
			IProtocol(std::move(identity), scheduler, std::move(wire)), lifetime(lifetime) {
		initialize();
	}

	Protocol::Protocol(Identities::IdKind kind, IScheduler *scheduler, std::shared_ptr<IWire> wire, Lifetime lifetime) :
			IProtocol(std::make_shared<Identities>(kind), scheduler, std::move(wire)), lifetime(lifetime) {
		initialize();
	}

	const SerializationCtx &Protocol::get_serialization_context() const {
		if (!context) {
			context = std::make_unique<SerializationCtx>(serializers.get(), SerializationCtx::roots_t{{util::getPlatformIndependentHash("Protocol"), internRoot.get()}});
		}
		return *context;
	}

}

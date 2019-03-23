#include "Protocol.h"

#include "SerializationCtx.h"
#include "InternRoot.h"

#include <utility>

namespace rd {
	const Logger Protocol::initializationLogger;

	void Protocol::initialize() const {
		internRoot = std::make_unique<InternRoot>();
		
		context = std::make_unique<SerializationCtx>(serializers.get(), SerializationCtx::roots_t{{util::getPlatformIndependentHash("Protocol"), internRoot.get()}});


		internRoot->rdid = RdId::Null().mix(InternRootName);
		scheduler->queue([this] {
			internRoot->bind(lifetime, this, InternRootName);
		});
	}

	Protocol::Protocol(std::shared_ptr<Identities> identity, IScheduler *scheduler, std::shared_ptr<IWire> wire, Lifetime lifetime) :
			IProtocol(std::move(identity), scheduler, std::move(wire)), lifetime(lifetime) {
		// initialize();
	}

	Protocol::Protocol(Identities::IdKind kind, IScheduler *scheduler, std::shared_ptr<IWire> wire, Lifetime lifetime) :
			IProtocol(std::make_shared<Identities>(kind), scheduler, std::move(wire)), lifetime(lifetime) {
		// initialize();
	}

	Protocol::~Protocol() = default;

	const SerializationCtx &Protocol::get_serialization_context() const {
		if (!context) {
			initialize();
		}
		return *context;
	}

}

//
// Created by jetbrains on 30.07.2018.
//

#include <utility>

#include "IScheduler.h"
#include "Identities.h"
#include "IWire.h"
#include "IProtocol.h"
#include "SerializationCtx.h"

namespace rd {
	IProtocol::IProtocol() {}

	IProtocol::IProtocol(std::shared_ptr<Identities> identity, IScheduler *scheduler, std::shared_ptr<IWire> wire) :
		identity(std::move(identity)),
		scheduler(scheduler),
		wire(std::move(wire)) {}

	const IProtocol *IProtocol::get_protocol() const {
		return this;
	}

	IScheduler *IProtocol::get_scheduler() const {
		return scheduler;
	}

	const IWire *IProtocol::get_wire() const {
		return wire.get();
	}

	const Serializers &IProtocol::get_serializers() const {
		return *serializers;
	}

	const Identities *IProtocol::get_identity() const {
		return identity.get();
	}

	IProtocol::~IProtocol() = default;
}

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
	IProtocol::IProtocol() {
		context = std::make_unique<SerializationCtx>();
	}

	IProtocol::IProtocol(std::shared_ptr<Identities> identity, IScheduler *scheduler, std::shared_ptr<IWire> wire) :
			identity(std::move(identity)),
			scheduler(scheduler),
			wire(std::move(wire)),
			context(std::make_unique<SerializationCtx>(serializers.get())) {}

	const SerializationCtx &IProtocol::get_serialization_context() const {
		return *context;
	}

	IProtocol::~IProtocol() = default;
}

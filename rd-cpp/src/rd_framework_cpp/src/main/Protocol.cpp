//
// Created by jetbrains on 25.07.2018.
//

#include "Protocol.h"

namespace rd {
	const Logger Protocol::initializationLogger;

	const IProtocol *Protocol::get_protocol() const {
		return this;
	}

	Protocol::Protocol(std::shared_ptr<IIdentities> identity, IScheduler *scheduler,
					   std::shared_ptr<IWire> wire) :
			IProtocol(std::move(identity), scheduler, std::move(wire)) {}

	Protocol::Protocol(Identities &&identity, IScheduler *scheduler, std::shared_ptr<IWire> wire) :
			IProtocol(std::make_shared<Identities>(std::move(identity)), scheduler, std::move(wire)) {}

}

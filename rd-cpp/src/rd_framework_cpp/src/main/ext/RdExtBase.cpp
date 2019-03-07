//
// Created by jetbrains on 24.07.2018.
//

#include "Lifetime.h"
#include "RdPropertyBase.h"
#include "RdExtBase.h"
#include "Protocol.h"

namespace rd {
	const IProtocol *RdExtBase::get_protocol() const {
		return extProtocol ? extProtocol.get() : RdReactiveBase::get_protocol();
	}

//must be overriden if derived ext have bindable members
	void RdExtBase::init(Lifetime lifetime) const {
//    Protocol.initializationLogger.traceMe { "binding" }

		auto parentProtocol = RdReactiveBase::get_protocol();
		std::shared_ptr<IWire> parentWire = parentProtocol->wire;

//    serializersOwner.registry(parentProtocol.serializers);

		auto sc = parentProtocol->get_scheduler();
		extWire->realWire = parentWire.get();
		lifetime->bracket(
				[&] {
					extProtocol = std::make_shared<Protocol>(parentProtocol->identity, sc,
															 std::static_pointer_cast<IWire>(extWire), lifetime);
				},
				[this] {
					extProtocol = nullptr;
				}
		);

		parentWire->advise(lifetime, this);

		//it's critical to advise before 'Ready' is sent because we advise on SynchronousScheduler

		lifetime->bracket(
				[this, parentWire] {
					sendState(*parentWire, ExtState::Ready);
				},
				[this, parentWire] {
					sendState(*parentWire, ExtState::Disconnected);
				}
		);

		for (auto const &it : bindable_extensions) {
			bindPolymorphic(*(it.second), lifetime, this, it.first);
		}

		traceMe(Protocol::initializationLogger, "created and bound :: ${printToString()}");
	}

	void RdExtBase::on_wire_received(Buffer buffer) const {
		ExtState remoteState = buffer.readEnum<ExtState>();
		traceMe(logReceived, "remote: " + to_string(remoteState));


		switch (remoteState) {
			case ExtState::Ready : {
				sendState(*extWire->realWire, ExtState::ReceivedCounterpart);
				extWire->connected.set(true);
				break;
			}
			case ExtState::ReceivedCounterpart : {
				extWire->connected.set(true); //don't set anything if already set
				break;
			}
			case ExtState::Disconnected : {
				extWire->connected.set(false);
				break;
			}
		}

		int64_t counterpartSerializationHash = buffer.read_integral<int64_t>();
		if (serializationHash != counterpartSerializationHash) {
			//need to queue since outOfSyncModels is not synchronized
//        RdReactiveBase::get_protocol()->scheduler->queue([this](){ RdReactiveBase::get_protocol().outOfSyncModels.add(this) });
			MY_ASSERT_MSG(false, "serializationHash of ext " + location.toString() +
								 " doesn't match to counterpart: maybe you forgot to generate models?")
		}
	}

	void RdExtBase::sendState(IWire const &wire, ExtState state) const {

		wire.send(rdid, [&](Buffer const &buffer) {
			// traceMe(logSend, to_string(state));
			buffer.writeEnum<ExtState>(state);
			buffer.write_integral<int64_t>(serializationHash);
		});
	}

	void RdExtBase::traceMe(const Logger &logger, std::string const &message) const {
		logger.trace("ext " + location.toString() + " " + rdid.toString() + ":: " + message);
	}

	std::string to_string(RdExtBase::ExtState state) {
		switch (state) {
			case RdExtBase::ExtState::Ready:
				return "Ready";
			case RdExtBase::ExtState::ReceivedCounterpart:
				return "ReceivedCounterpart";
			case RdExtBase::ExtState::Disconnected:
				return "Disconnected";
		}
		return {};
	}
}

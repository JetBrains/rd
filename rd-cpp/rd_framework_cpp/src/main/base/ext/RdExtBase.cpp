//
// Created by jetbrains on 24.07.2018.
//

#include "Lifetime.h"
#include "RdPropertyBase.h"
#include "RdExtBase.h"
#include "Protocol.h"

const IProtocol *const RdExtBase::get_protocol() const {
    return extProtocol ? extProtocol.get() : RdReactiveBase::get_protocol();
}


void RdExtBase::init(Lifetime lifetime) const {
//    Protocol.initializationLogger.traceMe { "binding" }

    auto parentProtocol = RdReactiveBase::get_protocol();
    std::shared_ptr<IWire> parentWire = parentProtocol->wire;

//    serializersOwner.registry(parentProtocol.serializers);

    auto sc = parentProtocol->scheduler;
    extWire->realWire = parentWire.get();
    lifetime->bracket(
            [&]() {
                extProtocol = std::make_shared<Protocol>(parentProtocol->identity, sc,
                                                          std::dynamic_pointer_cast<IWire>(extWire));
            },
            [this]() {
                extProtocol = nullptr;
            }
    );

    parentWire->advise(lifetime, this);

    //it's critical to advise before 'Ready' is sent because we advise on SynchronousScheduler

    lifetime->bracket(
            [this, parentWire]() {
                sendState(*parentWire, ExtState::Ready);
            },
            [this, parentWire]() {
                sendState(*parentWire, ExtState::Disconnected);
            }
    );


    //todo make it smarter
	//for (auto const &[name, child] : this->bindableChildren) {
    for (auto const &it : this->bindableChildren) {
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

    int64_t counterpartSerializationHash = buffer.read_pod<int64_t>();
    /*if (serializationHash != counterpartSerializationHash) {
        //need to queue since outOfSyncModels is not synchronized
        RdReactiveBase::get_protocol()->scheduler->queue([this](){ RdReactiveBase::get_protocol().outOfSyncModels.add(this) });
//        error("serializationHash of ext '$location' doesn't match to counterpart: maybe you forgot to generate models?")
    }*/
}

void RdExtBase::sendState(IWire const &wire, RdExtBase::ExtState state) const {

    wire.send(rdid, [&](Buffer const &buffer) {
//            logSend.traceMe(state);
        buffer.writeEnum<ExtState>(state);
        buffer.write_pod<int64_t>(serializationHash);
    });
}

void RdExtBase::traceMe(const Logger &logger, std::string const &message) const {
    logger.trace("ext " + location.toString() + " " + rdid.toString() + ":: " + message);
}

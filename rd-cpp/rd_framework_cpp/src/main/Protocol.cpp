//
// Created by jetbrains on 25.07.2018.
//

#include "Protocol.h"

IProtocol const *const Protocol::get_protocol() const {
    return this;
}

Protocol::Protocol(std::shared_ptr<IIdentities> identity, const IScheduler *const scheduler,
                   std::shared_ptr<IWire> wire) :
        IProtocol(std::move(identity), scheduler, std::move(wire)) {}

Protocol::Protocol(Identities &&identity, const IScheduler *const scheduler, std::shared_ptr<IWire> wire) :
        IProtocol(std::make_shared<Identities>(identity), scheduler, std::move(wire)) {}

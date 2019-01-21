//
// Created by jetbrains on 30.07.2018.
//

#include <utility>

#include "IScheduler.h"
#include "IIdentities.h"
#include "IWire.h"
#include "IProtocol.h"

IProtocol::IProtocol(std::shared_ptr<IIdentities> identity, IScheduler *scheduler, std::shared_ptr<IWire> wire) :
        identity(std::move(identity)),
        scheduler(scheduler),
        wire(std::move(wire)),
        context(&serializers) {}

const SerializationCtx &IProtocol::get_serialization_context() const {
    return context;
}

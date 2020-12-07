#include "IProtocol.h"

#include <utility>

namespace rd
{
IProtocol::IProtocol() = default;

IProtocol::IProtocol(std::shared_ptr<Identities> identity, IScheduler* scheduler, std::shared_ptr<IWire> wire)
	: identity(std::move(identity)), scheduler(scheduler), wire(std::move(wire))
{
}

IProtocol::~IProtocol() = default;
}	 // namespace rd

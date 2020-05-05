#ifndef RD_CPP_WIREBASE_H
#define RD_CPP_WIREBASE_H

#include "base/IWire.h"
#include "protocol/MessageBroker.h"
#include "reactive/Property.h"

namespace rd
{
class WireBase : public IWire
{
protected:
	IScheduler* scheduler = nullptr;

	MessageBroker message_broker;

public:
	// region ctor/dtor

	WireBase(WireBase&&) = default;

	explicit WireBase(IScheduler* scheduler) : scheduler(scheduler), message_broker(scheduler)
	{
	}

	virtual ~WireBase() = default;
	// endregion

	void advise(Lifetime lifetime, IRdReactive const* entity) const override;
};
}	 // namespace rd

#endif	  // RD_CPP_WIREBASE_H

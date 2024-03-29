#ifndef RD_CPP_EXTWIRE_H
#define RD_CPP_EXTWIRE_H

#include "base/IWire.h"
#include "protocol/RdId.h"
#include "protocol/Buffer.h"

#include <queue>
#include <mutex>
#include <functional>

#include <rd_framework_export.h>

RD_PUSH_STL_EXPORTS_WARNINGS

namespace rd
{
class RD_FRAMEWORK_API ExtWire final : public IWire
{
	mutable std::mutex lock;

	mutable std::queue<std::pair<RdId, Buffer::ByteArray> > sendQ;

public:
	ExtWire();

	mutable IWire const* realWire = nullptr;

	void advise(Lifetime lifetime, RdReactiveBase const* entity) const override;

	void send(RdId const& id, std::function<void(Buffer& buffer)> writer) const override;
};
}	 // namespace rd

RD_POP_STL_EXPORTS_WARNINGS

#endif	  // RD_CPP_EXTWIRE_H

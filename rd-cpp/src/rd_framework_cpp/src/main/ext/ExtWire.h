#ifndef RD_CPP_EXTWIRE_H
#define RD_CPP_EXTWIRE_H

#include "IWire.h"
#include "RdId.h"
#include "Buffer.h"

#include <queue>
#include <mutex>
#include <functional>

namespace rd {
	class ExtWire : public IWire {
		mutable std::mutex lock;

		mutable std::queue<std::pair<RdId, Buffer::ByteArray> > sendQ;
	public:
		ExtWire();

		mutable IWire const *realWire = nullptr;

		void advise(Lifetime lifetime, IRdReactive const *entity) const override;

		void send(RdId const &id, std::function< void(Buffer &buffer)> writer) const override;
	};
}


#endif //RD_CPP_EXTWIRE_H

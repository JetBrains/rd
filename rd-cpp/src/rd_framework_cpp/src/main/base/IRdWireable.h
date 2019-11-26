#ifndef RD_CPP_IRDWIREABLE_H
#define RD_CPP_IRDWIREABLE_H


#include "protocol/RdId.h"
#include "scheduler/base/IScheduler.h"
#include "protocol/Buffer.h"

namespace rd {
	class IRdWireable {
	public:
		mutable RdId rdid = RdId::Null();
	};
}


#endif //RD_CPP_IRDWIREABLE_H

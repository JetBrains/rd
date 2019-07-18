#ifndef RD_CPP_IRDWIREABLE_H
#define RD_CPP_IRDWIREABLE_H


#include <RdId.h>
#include "IScheduler.h"
#include "Buffer.h"

namespace rd {
	class IRdWireable {
	public:
		mutable RdId rdid = RdId::Null();
	};
}


#endif //RD_CPP_IRDWIREABLE_H

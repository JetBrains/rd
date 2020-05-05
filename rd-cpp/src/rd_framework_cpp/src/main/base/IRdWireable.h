#ifndef RD_CPP_IRDWIREABLE_H
#define RD_CPP_IRDWIREABLE_H

#include "protocol/Buffer.h"
#include "protocol/RdId.h"
#include "scheduler/base/IScheduler.h"

namespace rd
{
class IRdWireable
{
public:
	mutable RdId rdid = RdId::Null();
};
}	 // namespace rd

#endif	  // RD_CPP_IRDWIREABLE_H

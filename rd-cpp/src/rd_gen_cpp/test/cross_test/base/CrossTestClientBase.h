#ifndef RD_CPP_CROSSTESTCLIENTBASE_H
#define RD_CPP_CROSSTESTCLIENTBASE_H

#include "CrossTestBase.h"

#include "lifetime/LifetimeDefinition.h"
#include "scheduler/SimpleScheduler.h"
#include "base/IWire.h"
#include "wire/SocketWire.h"
#include "std/filesystem.h"
#include "protocol/Protocol.h"
#include "impl/RdProperty.h"

#include <fstream>

namespace rd
{
namespace cross
{
class CrossTestClientBase : public CrossTestBase
{
protected:
	CrossTestClientBase();

	template <typename T>
	void print(printer_t& printer, T const& x)
	{
		printer.push_back(to_string(x));
	}
};
}	 // namespace cross
}	 // namespace rd

#endif	  // RD_CPP_CROSSTESTCLIENTBASE_H

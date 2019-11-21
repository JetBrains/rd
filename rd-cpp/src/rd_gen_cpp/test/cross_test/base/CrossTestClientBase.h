#ifndef RD_CPP_CROSSTESTCLIENTBASE_H
#define RD_CPP_CROSSTESTCLIENTBASE_H

#include "CrossTestBase.h"

#include "LifetimeDefinition.h"
#include "SimpleScheduler.h"
#include "IWire.h"
#include "SocketWire.h"
#include "filesystem.h"
#include "Protocol.h"
#include "RdProperty.h"

#include <fstream>

namespace rd {
	namespace cross {
		class CrossTestClientBase : public CrossTestBase {
		protected:
			CrossTestClientBase();

			template<typename T>
			void print(printer_t &printer, T const &x) {
				printer.push_back(to_string(x));
			}
		};
	}
}

#endif //RD_CPP_CROSSTESTCLIENTBASE_H

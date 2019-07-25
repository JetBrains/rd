#ifndef RD_CPP_RDASYNCTESTBASE_H
#define RD_CPP_RDASYNCTESTBASE_H

#include "RdFrameworkTestBase.h"

#include "TestSingleThreadScheduler.h"

namespace rd {
	namespace test {
		namespace util {
			class RdAsyncTestBase : public RdFrameworkTestBase {
				TestSingleThreadScheduler clientBgScheduler{"ClientBg"};
				TestSingleThreadScheduler clientUiScheduler{"ClientUi"};
				TestSingleThreadScheduler serverBgScheduler{"ServerBg"};
				TestSingleThreadScheduler serverUiScheduler{"ServerUi"};
			};
		}
	}
}


#endif //RD_CPP_RDASYNCTESTBASE_H

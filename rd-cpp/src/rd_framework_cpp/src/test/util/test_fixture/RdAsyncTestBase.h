#ifndef RD_CPP_RDASYNCTESTBASE_H
#define RD_CPP_RDASYNCTESTBASE_H

#include "RdFrameworkTestBase.h"

#include <scheduler/TestSingleThreadScheduler.h>

namespace rd
{
namespace test
{
namespace util
{
class RdAsyncTestBase : public RdFrameworkTestBase
{
protected:
	std::unique_ptr<IScheduler> clientBgScheduler = std::make_unique<TestSingleThreadScheduler>("ClientBg");
	std::unique_ptr<IScheduler> clientUiScheduler = std::make_unique<TestSingleThreadScheduler>("ClientUi");
	std::unique_ptr<IScheduler> serverBgScheduler = std::make_unique<TestSingleThreadScheduler>("ServerBg");
	std::unique_ptr<IScheduler> serverUiScheduler = std::make_unique<TestSingleThreadScheduler>("ServerUi");
};
}	 // namespace util
}	 // namespace test
}	 // namespace rd

#endif	  // RD_CPP_RDASYNCTESTBASE_H

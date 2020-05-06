#ifndef RD_CPP_TESTSINGLETHREADSCHEDULER_H
#define RD_CPP_TESTSINGLETHREADSCHEDULER_H

#include "scheduler/base/SingleThreadSchedulerBase.h"

namespace rd
{
class TestSingleThreadScheduler : public SingleThreadSchedulerBase
{
public:
	// region ctor/dtor
	explicit TestSingleThreadScheduler(std::string string);

	virtual ~TestSingleThreadScheduler() = default;
	// endregion
};
}	 // namespace rd

#endif	  // RD_CPP_TESTSINGLETHREADSCHEDULER_H

#ifndef RD_CPP_TESTSINGLETHREADSCHEDULER_H
#define RD_CPP_TESTSINGLETHREADSCHEDULER_H

#include "SingleThreadSchedulerBase.h"

namespace rd {
	class TestSingleThreadScheduler : public SingleThreadSchedulerBase {

	public:
		TestSingleThreadScheduler(std::string string);
	};
}


#endif //RD_CPP_TESTSINGLETHREADSCHEDULER_H

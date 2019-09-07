#ifndef RD_CPP_SINGLETHREADSCHEDULER_H
#define RD_CPP_SINGLETHREADSCHEDULER_H


#include "SingleThreadSchedulerBase.h"

#include "Lifetime.h"

namespace rd {
	class SingleThreadScheduler : public SingleThreadSchedulerBase {
	public:
		Lifetime lifetime;

		SingleThreadScheduler(Lifetime lifetime, std::string name);
	};
}


#endif //RD_CPP_SINGLETHREADSCHEDULER_H

#include "TestSingleThreadScheduler.h"

#include <utility>

namespace rd {
	TestSingleThreadScheduler::TestSingleThreadScheduler(std::string string) :
			SingleThreadSchedulerBase(std::move(string)) {}
}

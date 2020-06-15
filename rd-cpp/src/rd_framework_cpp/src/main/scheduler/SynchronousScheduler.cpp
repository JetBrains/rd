#include "SynchronousScheduler.h"

#include "guards.h"

namespace rd
{
thread_local int32_t SynchronousScheduler::active = 0;

void SynchronousScheduler::queue(std::function<void()> action)
{
	util::increment_guard<int32_t> guard(active);
	action();
}

void SynchronousScheduler::flush()
{
}

bool SynchronousScheduler::is_active() const
{
	return active > 0;
}

SynchronousScheduler globalSynchronousScheduler;
}	 // namespace rd
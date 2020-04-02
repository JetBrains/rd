#include "SingleThreadSchedulerBase.h"

#include "ctpl_stl.h"

namespace rd {
	SingleThreadSchedulerBase::PoolTask::PoolTask(std::function<void()> f, SingleThreadSchedulerBase *scheduler) :
			f(std::move(f)), scheduler(scheduler) {}

	void SingleThreadSchedulerBase::PoolTask::operator()(int id) const {
		try {
			f();
			--scheduler->tasks_executing;
		} catch (std::exception const &e) {
			scheduler->log.error(&e, "Background task failed, scheduler=%s, thread_id=%d",
								 scheduler->name.c_str(), id);
			--scheduler->tasks_executing;
		}
	}

	SingleThreadSchedulerBase::SingleThreadSchedulerBase(std::string name) :
			name(std::move(name)), pool(std::make_unique<ctpl::thread_pool>(1))
	{
		thread_id = std::this_thread::get_id();
	}

	void SingleThreadSchedulerBase::flush() {
		RD_ASSERT_MSG(!is_active(),
					  "Can't flush this scheduler in a reentrant way: we are inside queued item's execution");

		while (tasks_executing != 0) {
			std::this_thread::yield();
		}
	}

	void SingleThreadSchedulerBase::queue(std::function<void()> action) {
		++tasks_executing;
		PoolTask task(action, this);
		pool->push(std::move(task));
	}

	bool SingleThreadSchedulerBase::is_active() const {
		return thread_id == std::this_thread::get_id();
	}

	SingleThreadSchedulerBase::~SingleThreadSchedulerBase() = default;
}
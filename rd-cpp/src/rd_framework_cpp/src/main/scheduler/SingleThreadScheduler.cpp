#include "SingleThreadScheduler.h"

#include "ctpl_stl.h"

namespace rd {
	SingleThreadScheduler::PoolTask::PoolTask(std::function<void()> f, SingleThreadScheduler *scheduler) :
			f(std::move(f)), scheduler(scheduler) {}

	void SingleThreadScheduler::PoolTask::operator()(int id) const {
		try {
			f();
			--scheduler->tasks_executing;
		} catch (std::exception const &e) {
			scheduler->log.error(&e, "Background task failed, scheduler=%s, thread_id=%d",
								 scheduler->name.c_str(), id);
			--scheduler->tasks_executing;
		}
	}

	SingleThreadScheduler::SingleThreadScheduler(Lifetime lifetime, std::string name) :
			lifetime(lifetime), name(std::move(name)), pool(std::make_unique<ctpl::thread_pool>(1)) {
		lifetime->add_action([this]() {
			try {
				pool->stop(true);
			} catch (std::exception const &e) {
				log.error("Failed to terminate %s", this->name.c_str());
			}
		});
	}

	void SingleThreadScheduler::flush() {
		RD_ASSERT_MSG(!is_active(),
					  "Can't flush this scheduler in a reentrant way: we are inside queued item's execution");

		while (tasks_executing != 0) {
			std::this_thread::yield();
		}
	}

	void SingleThreadScheduler::queue(std::function<void()> action) {
		++tasks_executing;
		PoolTask task(action, this);
		pool->push(std::move(task));
	}

	bool SingleThreadScheduler::is_active() const {
		return active > 0;
	}

	SingleThreadScheduler::~SingleThreadScheduler() = default;
}
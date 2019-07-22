#ifndef RD_CPP_SINGLETHREADSCHEDULER_H
#define RD_CPP_SINGLETHREADSCHEDULER_H

#include "IScheduler.h"
#include "Lifetime.h"
#include "ctpl_stl.h"

#include <utility>

namespace rd {
	class SingleThreadScheduler : public IScheduler {
	private:
		Logger log;

		Lifetime lifetime;
		std::string name;

		std::atomic_uint32_t tasks_executing{0};
		std::atomic_uint32_t active{0};
		ctpl::thread_pool pool{1};

		class PoolTask {
			std::function<void()> f;
			SingleThreadScheduler *scheduler;
		public:
			explicit PoolTask(std::function<void()> f, SingleThreadScheduler *scheduler) :
					f(std::move(f)), scheduler(scheduler) {}

			void operator()(int id) const {
				try {
					f();
					--scheduler->tasks_executing;
				} catch (std::exception const &e) {
					scheduler->log.error(&e, "Background task failed, scheduler=%s, thread_id=%d",
										 scheduler->name.c_str(), id);
					--scheduler->tasks_executing;
				}
			}
		};

	public:
		SingleThreadScheduler(Lifetime lifetime, std::string name) :
				lifetime(lifetime), name(std::move(name)) {
			lifetime->add_action([this]() {
				try {
					pool.stop(true);
				} catch (std::exception const &e) {
					log.error("Failed to terminate %s", this->name.c_str());
				}
			});
		}

		void flush() override {
			RD_ASSERT_MSG(!is_active(),
						  "Can't flush this scheduler in a reentrant way: we are inside queued item's execution");

			while (tasks_executing != 0) {
				std::this_thread::yield();
			}
		};

		void queue(std::function<void()> action) override {
			++tasks_executing;
			PoolTask task(action, this);
			pool.push(std::move(task));
		};

		bool is_active() const override {
			return active > 0;
		};
	};
}


#endif //RD_CPP_SINGLETHREADSCHEDULER_H

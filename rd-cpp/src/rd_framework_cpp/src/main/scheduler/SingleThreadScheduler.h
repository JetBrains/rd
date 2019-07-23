#ifndef RD_CPP_SINGLETHREADSCHEDULER_H
#define RD_CPP_SINGLETHREADSCHEDULER_H

#include "IScheduler.h"
#include "Lifetime.h"

#include <utility>

namespace ctpl {
	class thread_pool;
}

namespace rd {
	class SingleThreadScheduler : public IScheduler {
	private:
		Logger log;

		Lifetime lifetime;
		std::string name;

		std::atomic_uint32_t tasks_executing{0};
		std::atomic_uint32_t active{0};
		std::unique_ptr<ctpl::thread_pool> pool;

		class PoolTask {
			std::function<void()> f;
			SingleThreadScheduler *scheduler;
		public:
			explicit PoolTask(std::function<void()> f, SingleThreadScheduler *scheduler);

			void operator()(int id) const;
		};

	public:
		SingleThreadScheduler(Lifetime lifetime, std::string name);

		virtual ~SingleThreadScheduler();

		void flush() override;

		void queue(std::function<void()> action) override;

		bool is_active() const override;
	};
}


#endif //RD_CPP_SINGLETHREADSCHEDULER_H

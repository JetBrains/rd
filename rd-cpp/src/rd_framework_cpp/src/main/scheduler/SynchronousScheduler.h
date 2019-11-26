#ifndef RD_CPP_SYNCHRONOUSSCHEDULER_H
#define RD_CPP_SYNCHRONOUSSCHEDULER_H

#include "scheduler/base/IScheduler.h"

#include "guards.h"

namespace rd {
	class SynchronousScheduler : public IScheduler {
		static thread_local int32_t active;
	public:
		//region ctor/dtor

		SynchronousScheduler() = default;

		SynchronousScheduler(SynchronousScheduler const &) = delete;

		SynchronousScheduler(SynchronousScheduler &&) = delete;

		virtual ~SynchronousScheduler() = default;
		//endregion

		void queue(std::function<void()> action) override;

		void flush() override;

		bool is_active() const override;
	};

	/**
	 * \brief global synchronous scheduler for whole application.
	 */
	extern SynchronousScheduler globalSynchronousScheduler;
}


#endif //RD_CPP_SYNCHRONOUSSCHEDULER_H

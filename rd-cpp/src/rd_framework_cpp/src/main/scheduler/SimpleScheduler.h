#ifndef RD_CPP_TESTSCHEDULER_H
#define RD_CPP_TESTSCHEDULER_H

#include "scheduler/base/IScheduler.h"

namespace rd {
	/**
	 * \brief simple scheduler, which immediately invoke action on queue, and is always active.
	 */
	class SimpleScheduler : public IScheduler {
	public:
		//region ctor/dtor
		SimpleScheduler() = default;

		virtual ~SimpleScheduler() = default;
		//endregion

		void flush() override;

		void queue(std::function<void()> action) override;

		bool is_active() const override;

	};
}

#endif //RD_CPP_TESTSCHEDULER_H

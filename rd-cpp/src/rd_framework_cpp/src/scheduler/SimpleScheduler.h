#ifndef RD_CPP_TESTSCHEDULER_H
#define RD_CPP_TESTSCHEDULER_H

#include "IScheduler.h"

namespace rd {
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

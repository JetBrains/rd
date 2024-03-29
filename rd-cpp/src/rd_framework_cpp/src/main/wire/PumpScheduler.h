#ifndef RD_CPP_PUMPSCHEDULER_H
#define RD_CPP_PUMPSCHEDULER_H

#include "scheduler/base/IScheduler.h"

#include <condition_variable>
#include <string>
#include <thread>
#include <queue>

#include <rd_framework_export.h>

RD_PUSH_STL_EXPORTS_WARNINGS

namespace rd
{
namespace test
{
namespace util
{
class RD_FRAMEWORK_API PumpScheduler : public IScheduler
{
public:
	std::string name;

	mutable std::condition_variable cv;
	mutable std::mutex lock;

	std::thread::id created_thread_id;
	mutable std::queue<std::function<void()> > messages;

	// region ctor/dtor

	PumpScheduler();

	explicit PumpScheduler(std::string const& name);

	virtual ~PumpScheduler() = default;
	// endregion

	void flush() override;

	void queue(std::function<void()> action) override;

	bool is_active() const override;

	void assert_thread() const override;

	void pump_one_message();
};
}	 // namespace util
}	 // namespace test
}	 // namespace rd

RD_POP_STL_EXPORTS_WARNINGS

#endif	  // RD_CPP_PUMPSCHEDULER_H

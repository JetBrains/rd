#ifndef RD_CPP_ISCHEDULER_H
#define RD_CPP_ISCHEDULER_H

#include <functional>
#include <thread>

#include <rd_framework_export.h>

RD_PUSH_STL_EXPORTS_WARNINGS

namespace rd
{
/**
 * \brief Allows to queue the execution of actions on a different thread.
 */
class RD_FRAMEWORK_API IScheduler
{
protected:
	std::thread::id thread_id;

public:
	// region ctor/dtor

	IScheduler() = default;

	virtual ~IScheduler() = default;
	// endregion

	/**
	 * \brief Queues the execution of the given [action].
	 *
	 * \param action to be queued.
	 */
	virtual void queue(std::function<void()> action) = 0;

	// TO-DO
	bool out_of_order_execution = false;

	virtual void assert_thread() const;

	/**
	 * \brief invoke action immediately if scheduler is active, queue it otherwise.
	 * \param action to be invoked
	 */
	virtual void invoke_or_queue(std::function<void()> action);

	virtual void flush() = 0;

	virtual bool is_active() const = 0;

	std::thread::id get_thread_id() const
	{
		return thread_id;
	}
};
}	 // namespace rd

RD_POP_STL_EXPORTS_WARNINGS

#endif	  // RD_CPP_ISCHEDULER_H

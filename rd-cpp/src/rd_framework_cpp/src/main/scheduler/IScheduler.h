#ifndef RD_CPP_ISCHEDULER_H
#define RD_CPP_ISCHEDULER_H


#include "Logger.h"

#include <functional>

namespace rd {
	/**
	 * \brief Allows to queue the execution of actions on a different thread.
	 */
	class IScheduler {
		static Logger logger;
	public:
		//region ctor/dtor

		IScheduler() = default;

		virtual ~IScheduler() = default;
		//endregion

		/**
		 * \brief Queues the execution of the given [action].
		 * 
		 * \param action to be queued.
		 */
		virtual void queue(std::function<void()> action) = 0;

		//todo
		bool out_of_order_execution = false;

		virtual void assert_thread() const;

		/**
		 * \brief invoke action immediately if scheduler is active, queue it otherwise.
		 * \param action to be invoked
		 */
		virtual void invoke_or_queue(std::function<void()> action);

		virtual void flush() = 0;

		virtual bool is_active() const = 0;
	};
}


#endif //RD_CPP_ISCHEDULER_H

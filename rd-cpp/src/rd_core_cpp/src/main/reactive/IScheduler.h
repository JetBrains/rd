#ifndef RD_CPP_ISCHEDULER_H
#define RD_CPP_ISCHEDULER_H


#include "Logger.h"

#include <functional>

namespace rd {
	class IScheduler {
		static Logger logger;
	public:
		//region ctor/dtor

		IScheduler() = default;

		virtual ~IScheduler() = default;
		//endregion

		virtual void queue(std::function<void()> action) = 0;

		bool out_of_order_execution = false;

		virtual void assert_thread() const;

		void invoke_or_queue(std::function<void()> action);

		virtual void flush() = 0;

		virtual bool is_active() const = 0;
	};
}


#endif //RD_CPP_ISCHEDULER_H

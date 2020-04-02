#include "IScheduler.h"

#include <functional>

#include "fmt/format.h"
#include "fmt/ostream.h"

namespace rd {
	void IScheduler::assert_thread() const {
		if (!is_active()) {
			Logger().error(fmt::format("Illegal scheduler for current action. Must be {}, was {}", thread_id, std::this_thread::get_id()));
		}
	}

	void IScheduler::invoke_or_queue(std::function<void()> action) {
		if (is_active()) {
			action();
		} else {
			queue(action);
		}
	}
}

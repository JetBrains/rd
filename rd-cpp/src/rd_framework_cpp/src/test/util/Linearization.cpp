#include "Linearization.h"

#include "core_util.h"

#include <chrono>
#include <mutex>
#include <condition_variable>

using namespace std::chrono_literals;

namespace rd {
	void rd::util::Linearization::point(int32_t id) {
		std::cerr << "Enter point " + std::to_string(id) << std::endl;
		{
			std::unique_lock<decltype(lock)> ul{lock};
			auto status = cv.wait_for(ul, 1000ms, [&] {
				return enabled && id <= next_id;
			});

			if (!enabled) return;

			RD_ASSERT_MSG(id <= next_id, "Point " + to_string(id) + " already set, next_id=" + to_string(next_id));
			++next_id;
		}
		cv.notify_all();
		std::cerr << "Exit point " + std::to_string(id) << std::endl;
	}

	void util::Linearization::reset() {
		{
			std::lock_guard<decltype(lock)> guard{lock};
			next_id = 0;
		}
		cv.notify_all();
	}


	void util::Linearization::enable() {
		set_enable(true);
	}

	void util::Linearization::disable() {
		set_enable(false);
	}

	void util::Linearization::set_enable(bool value) {
		{
			std::lock_guard<decltype(lock)> guard{lock};
			enabled = value;
		}
		cv.notify_all();
	}
}
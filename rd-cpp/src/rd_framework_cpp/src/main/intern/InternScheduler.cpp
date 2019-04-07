#include "InternScheduler.h"

namespace rd {
	thread_local int32_t InternScheduler::active_counts = 0;

    InternScheduler::InternScheduler() {
        out_of_order_execution = true;
    }

    void InternScheduler::queue(std::function<void()> action) {
        ++active_counts;
		try {
			action();
		} catch (...) {
			
		}
        --active_counts;
        //todo RAII
    }

    void InternScheduler::flush() {}

    bool InternScheduler::is_active() const {
        return active_counts > 0;
    }
}

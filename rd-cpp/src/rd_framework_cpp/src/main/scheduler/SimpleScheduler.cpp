//
// Created by jetbrains on 03.10.2018.
//

#include "SimpleScheduler.h"

namespace rd {
	void SimpleScheduler::flush() {}

	void SimpleScheduler::queue(std::function<void()> action) {
		action();
	}

	bool SimpleScheduler::is_active() const {
		return true;
	}
}
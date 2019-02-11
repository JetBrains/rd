//
// Created by jetbrains on 03.10.2018.
//

#include "TestScheduler.h"

namespace rd {
	namespace test {
		namespace util {
			void TestScheduler::queue(std::function<void()> action) {
				action();
			}

			bool TestScheduler::is_active() const {
				return true;
			}
		}
	}
}
//
// Created by jetbrains on 23.07.2018.
//

#include <functional>

#include "IScheduler.h"

void IScheduler::assert_thread() const {
    if (!is_active()) {
        rd::Logger().error("Illegal scheduler for current action");
    }
}

void IScheduler::invoke_or_queue(std::function<void()> action) {
    if (is_active()) {
        action();
    } else {
        queue(action);
    }
}

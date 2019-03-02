//
// Created by jetbrains on 3/1/2019.
//

#include "InternScheduler.h"

namespace rd {
    thread_local int32_t active_counts = 0;
    //todo replace that!!!

    InternScheduler::InternScheduler() {
        out_of_order_execution = true;
    }

    void InternScheduler::queue(std::function<void()> action) {
        ++active_counts;
        action();
        --active_counts;
        //todo RAII
    }

    void InternScheduler::flush() {}

    bool InternScheduler::is_active() const {
        return active_counts > 0;
    }
}

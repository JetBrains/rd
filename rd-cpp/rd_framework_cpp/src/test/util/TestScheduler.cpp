//
// Created by jetbrains on 03.10.2018.
//

#include "TestScheduler.h"

void TestScheduler::queue(std::function<void()> action) {
    action();
}

bool TestScheduler::is_active() const {
    return true;
}
//
// Created by jetbrains on 03.10.2018.
//

#ifndef RD_CPP_TESTSCHEDULER_H
#define RD_CPP_TESTSCHEDULER_H

#include "IScheduler.h"

class TestScheduler : public IScheduler {
public:
    virtual ~TestScheduler() = default;

    void flush() const override {}

    void queue(std::function<void()> action) const override;

    bool is_active() const override;
};

#endif //RD_CPP_TESTSCHEDULER_H

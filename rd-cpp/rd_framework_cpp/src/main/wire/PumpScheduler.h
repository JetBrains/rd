//
// Created by jetbrains on 03.10.2018.
//

#ifndef RD_CPP_PUMPSCHEDULER_H
#define RD_CPP_PUMPSCHEDULER_H

#include <condition_variable>
#include <thread>
#include <queue>

#include "IScheduler.h"

class PumpScheduler : public IScheduler {
public:
    std::string name;

    mutable std::condition_variable cv;
    mutable std::mutex lock;

    std::thread::id created_thread_id;
    mutable std::queue<std::function<void()> > messages;

    //region ctor/dtor

    PumpScheduler();

    explicit PumpScheduler(std::string const &name);

    virtual ~PumpScheduler() = default;
    //endregion

    void flush() const override;

    void queue(std::function<void()> action) const override;

    bool is_active() const override;

    void assert_thread() const override;

    void pump_one_message() const;
};

#endif //RD_CPP_PUMPSCHEDULER_H

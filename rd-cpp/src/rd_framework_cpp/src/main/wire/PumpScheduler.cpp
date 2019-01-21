//
// Created by jetbrains on 03.10.2018.
//

#include "PumpScheduler.h"
#include "demangle.h"

PumpScheduler::PumpScheduler() : created_thread_id(std::this_thread::get_id()) {}

void PumpScheduler::flush() {
    assert_thread();

    auto action = std::move(messages.front());
    messages.pop();
    action();
}

void PumpScheduler::queue(std::function<void()> action) {
    {
        std::lock_guard<std::mutex> guard(lock);
        messages.push(std::move(action));
    }
    cv.notify_all();
}

bool PumpScheduler::is_active() const {
    return true;
}

void PumpScheduler::assert_thread() const {
    MY_ASSERT_MSG(created_thread_id == std::this_thread::get_id(),
                  "Illegal thread for current action, must be: " + to_string(created_thread_id) +
                  ", current thread: " + to_string(std::this_thread::get_id()));
}

void PumpScheduler::pump_one_message() {
    std::unique_lock<std::mutex> ul(lock);
    cv.wait(ul, [this]() -> bool { return !messages.empty(); });
    flush();
}

PumpScheduler::PumpScheduler(std::string const &name) : PumpScheduler() {
    this->name = name;
}
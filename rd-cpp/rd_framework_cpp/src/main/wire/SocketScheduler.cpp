//
// Created by jetbrains on 03.10.2018.
//

#include "SocketScheduler.h"
#include "demangle.h"

SocketScheduler::SocketScheduler() : created_thread_id(std::this_thread::get_id()) {}

void SocketScheduler::flush() const {
    assert_thread();

    auto action = std::move(messages.front());
    messages.pop();
    action();
}

void SocketScheduler::queue(std::function<void()> action) const {
    {
        std::lock_guard<std::mutex> _(lock);
        messages.push(std::move(action));
    }
    cv.notify_all();
}

bool SocketScheduler::is_active() const {
    return true;
}

void SocketScheduler::assert_thread() const {
    MY_ASSERT_MSG(created_thread_id == std::this_thread::get_id(),
                  "Illegal thread for current action, must be: " + to_string(created_thread_id) +
                  ", current thread: " + to_string(std::this_thread::get_id()));
}

void SocketScheduler::pump_one_message() const {
    std::unique_lock<std::mutex> ul(lock);
    cv.wait(ul, [this]() -> bool { return !messages.empty(); });
    flush();
}

SocketScheduler::SocketScheduler(std::string const &name) : SocketScheduler() {
    this->name = name;
}
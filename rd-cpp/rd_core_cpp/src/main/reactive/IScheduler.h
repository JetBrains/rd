//
// Created by jetbrains on 23.07.2018.
//

#ifndef RD_CPP_ISCHEDULER_H
#define RD_CPP_ISCHEDULER_H


#include <functional>

#include "Logger.h"

class IScheduler {
    Logger logger;
public:
    virtual void queue(std::function<void()> action) const = 0;

    /*void assertThread(debugInfo: Any? = null) {
        if (!isActive) {
            Logger.root.error {
                "Illegal scheduler for current action, must be: $this, current thread: ${currentThreadName()}" +
                (debugInfo?.let { ", debug info: $it" } ?:"")
            }
        }
    }*/

    bool out_of_order_execution = false;

    virtual void assert_thread() const;

    void invoke_or_queue(std::function<void()> action);

    virtual void flush() const = 0;

    virtual bool is_active() const = 0;
};


#endif //RD_CPP_ISCHEDULER_H

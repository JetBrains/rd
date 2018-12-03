//
// Created by jetbrains on 11.07.2018.
//

#ifndef RD_CPP_CORE_LOGGER_H
#define RD_CPP_CORE_LOGGER_H

#include "optional.hpp"

#include <functional>
#include <string>
#include <exception>
#include <iostream>

enum class LogLevel {
    Trace,
    Debug,
    Info,
    Warn,
    Error,
    Fatal
};

class Logger {
public:
    /*virtual */void log(LogLevel level, const std::string &message, std::exception const *e = nullptr)/* = 0;*/const {
        std::cerr << std::to_string(static_cast<int>(level))
                     //                     + " | " + std::to_string(GetCurrentThreadId())
                     + " | " + message +
                     +" | " + (e ? e->what() : "")
                  << std::endl;
    }

    void trace(std::string const &msg, std::exception const *e = nullptr) const {
        log(LogLevel::Trace, msg, e);
    }

    void debug(std::string const &msg, std::exception const *e = nullptr) const {
        log(LogLevel::Debug, msg, e);
    }

    void info(std::string const &msg, std::exception const *e = nullptr) const {
        log(LogLevel::Info, msg, e);
    }

    void error(std::string const &msg, std::exception const *e = nullptr) const {
        log(LogLevel::Error, msg, e);
    }

//    virtual bool is_enabled(LogLevel level) = 0;
};

/*class SwitchLogger : public Logger {

public:
    SwitchLogger(const std::string &category);

    void log(LogLevel level, std::string message, std::exception const &e) override;

    bool is_enabled(LogLevel level) override;
};*/

//SwitchLogger get_logger(std::string category);

template<typename F>
void catch_(tl::optional<std::string> comment, F &&action) {
    try {
        action();
    }
    catch (std::exception const &e) {
        std::string sfx = (comment.has_value() && !comment.value().empty()) ? comment.value() : " " + std::string(e.what());
//        get_logger("Default-Error-Logger").log(LogLevel::Error, "Catch$sfx", e);
    }
}

template<typename F>
void catch_(F &&action) {
    catch_({}, std::forward<F>(action));
}


#endif //RD_CPP_CORE_LOGGER_H

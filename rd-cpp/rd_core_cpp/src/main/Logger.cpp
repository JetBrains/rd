//
// Created by jetbrains on 11.07.2018.
//

#include "Logger.h"

/*SwitchLogger::SwitchLogger(const std::string &category) {}

SwitchLogger get_logger(std::string category) {
    return SwitchLogger(category);
}*/

/*void SwitchLogger::log(LogLevel level, std::string message, std::exception const &e) {
    realLogger.log(level, message, e);
}

bool SwitchLogger::is_enabled(LogLevel level) {
    return realLogger.isEnabled(level);
}*/

void catch_(tl::optional<std::string> comment, const std::function<void()> &action) {
    try {
        action();
    }
    catch (std::exception const &e) {
        std::string sfx = (comment.has_value() && !comment.value().empty()) ? comment.value() : " " + *e.what();
//        get_logger("Default-Error-Logger").log(LogLevel::Error, "Catch$sfx", e);
    }
}

void catch_(const std::function<void()> &action) {
    catch_({}, action);
}

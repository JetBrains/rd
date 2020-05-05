#include "Logger.h"

#include "util/core_util.h"

#include <iostream>
#include <thread>

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

namespace rd
{
// namespace log {
LogLevel minimum_level_to_log = LogLevel::Trace;
//}

std::mutex Logger::lock;

using namespace std::literals::string_literals;

std::string to_string(LogLevel level)
{
	switch (level)
	{
		case LogLevel::Trace:
			return "Trace";
		case LogLevel::Debug:
			return "Debug";
		case LogLevel::Info:
			return "Info ";
		case LogLevel::Warn:
			return "Warn ";
		case LogLevel::Error:
			return "Error";
		case LogLevel::Fatal:
			return "Fatal";
	}
	return {};
}
}	 // namespace rd

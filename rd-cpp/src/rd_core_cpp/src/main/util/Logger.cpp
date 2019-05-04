#include "Logger.h"

#include "core_util.h"

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

namespace rd {
	//namespace log {
	LogLevel minimum_level_to_log;
	//}
	using namespace std::literals::string_literals;

	std::string to_string(LogLevel level) {
		switch (level) {
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

	std::string string_format(string_view format, va_list args)
	{
		size_t len = std::vsnprintf(NULL, 0, format.data(), args);
		va_end(args);
		std::string vec;
		vec.resize(len + 1);
//		va_start(args, format);
		std::vsnprintf(&vec[0], len + 1, format.data(), args);
		va_end(args);
		return vec;
	}

	void Logger::log(LogLevel level, string_view format, va_list args, std::exception const *e) const {
		if (level >= minimum_level_to_log) {
			std::stringstream ss;
			ss << to_string(level) << " | " << to_string(std::this_thread::get_id()) << " | ";
			ss << string_format(format, args);
			if (e) {
				ss << " | " << e->what();
			}
			std::cerr << ss.str() << std::endl;
		}
	}

	void Logger::trace(const std::exception *e, string_view msg, ...) const {
		va_list args;
		va_start(args, msg);
		log(LogLevel::Error, msg, args, e);
	}

	void Logger::trace(string_view msg, ...) const {
		va_list args;
		va_start(args, msg);
		log(LogLevel::Error, msg, args);
	}

	void Logger::debug(const std::exception *e, string_view msg, ...) const {
		va_list args;
		va_start(args, msg);
		log(LogLevel::Error, msg, args, e);
	}

	void Logger::debug(string_view msg, ...) const {
		va_list args;
		va_start(args, msg);
		log(LogLevel::Error, msg, args);
	}

	void Logger::info(const std::exception *e, string_view msg, ...) const {
		va_list args;
		va_start(args, msg);
		log(LogLevel::Error, msg, args, e);
	}

	void Logger::info(string_view msg, ...) const {
		va_list args;
		va_start(args, msg);
		log(LogLevel::Error, msg, args);
	}

	void Logger::warn(const std::exception *e, string_view msg, ...) const {
		va_list args;
		va_start(args, msg);
		log(LogLevel::Error, msg, args, e);
	}

	void Logger::warn(string_view msg, ...) const {
		va_list args;
		va_start(args, msg);
		log(LogLevel::Error, msg, args);
	}

	void Logger::error(std::exception const *e, string_view msg, ...) const {
		va_list args;
		va_start(args, msg);
		log(LogLevel::Error, msg, args, e);
	}

	void Logger::error(string_view msg, ...) const {
		va_list args;
		va_start(args, msg);
		log(LogLevel::Error, msg, args);
	}
}

#ifndef RD_CPP_CORE_LOGGER_H
#define RD_CPP_CORE_LOGGER_H

#include "thirdparty.hpp"
#include "core_util.h"

#include <string>
#include <exception>
#include <iostream>
#include <thread>

namespace rd {
	enum class LogLevel {
		Trace,
		Debug,
		Info,
		Warn,
		Error,
		Fatal
	};

	extern LogLevel minimum_level_to_log;

	std::string to_string(LogLevel level);

	class Logger {
		static std::string string_format(string_view format, ...) {
			va_list args;
			size_t len = std::vsnprintf(NULL, 0, format.data(), args);
			va_end(args);
			std::string vec;
			vec.resize(len + 1);
			va_start(args, format);
			std::vsnprintf(&vec[0], len + 1, format.data(), args);
			va_end(args);
			return vec;
		}

	public:
		template<typename... Args>
		void log(std::exception const *e, LogLevel level, string_view format, Args &&... args) const {
			if (level >= minimum_level_to_log) {
				std::ostringstream ss;
				ss << to_string(level) << " | " << to_string(std::this_thread::get_id()) << " | ";
				ss << string_format(format, std::forward<Args>(args)...);
				if (e) {
					ss << " | " << e->what();
				}
				ss << "\n";
//				std::cerr << ss.str();
				std::cerr.flush();
			}
		}

		template<typename... Args>
		void trace(const std::exception *e, string_view msg, Args &&... args) const {
			log(e, LogLevel::Trace, msg, std::forward<Args>(args)...);
		}

		template<typename... Args>
		void trace(string_view msg, Args &&... args) const {
			log(nullptr, LogLevel::Trace, msg, std::forward<Args>(args)...);
		}

		template<typename... Args>
		void debug(const std::exception *e, string_view msg, Args &&... args) const {
			log(LogLevel::Debug, msg, std::forward<Args>(args)...);
		}

		template<typename... Args>
		void debug(string_view msg, Args &&... args) const {
			log(nullptr, LogLevel::Debug, msg, std::forward<Args>(args)...);
		}

		template<typename... Args>
		void info(const std::exception *e, string_view msg, Args &&... args) const {
			log(e, LogLevel::Info, msg, std::forward<Args>(args)...);
		}

		template<typename... Args>
		void info(string_view msg, Args &&... args) const {
			log(nullptr, LogLevel::Info, msg, std::forward<Args>(args)...);
		}

		template<typename... Args>
		void warn(const std::exception *e, string_view msg, Args &&... args) const {
			log(e, LogLevel::Warn, msg, std::forward<Args>(args)...);
		}

		template<typename... Args>
		void warn(string_view msg, Args &&... args) const {
			log(nullptr, LogLevel::Warn, msg, std::forward<Args>(args)...);
		}

		template<typename... Args>
		void error(std::exception const *e, string_view msg, Args &&... args) const {
			log(e, LogLevel::Error, msg, std::forward<Args>(args)...);
		}

		template<typename... Args>
		void error(string_view msg, Args &&... args) const {
			log(nullptr, LogLevel::Error, msg, std::forward<Args>(args)...);
		}
	};

	/*class SwitchLogger : public	Logger {

	public:
		SwitchLogger(const std::string &category);

		void log(LogLevel level, std::string message, std::exception const &e) override;

		bool is_enabled(LogLevel level) override;
	};*/

	//SwitchLogger get_logger(std::string category);

	template<typename F>
	void catch_(string_view comment, F &&action) {
		try {
			action();
		}
		catch (std::exception const &e) {
			Logger().error("Caught exception: %.*s %s", comment.length(), comment, e.what());
		}
	}

	template<typename F>
	void catch_(F &&action) {
		catch_({}, std::forward<F>(action));
	}

}

#endif //RD_CPP_CORE_LOGGER_H

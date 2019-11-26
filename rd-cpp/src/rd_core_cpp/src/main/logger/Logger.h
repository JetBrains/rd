#ifndef RD_CPP_CORE_LOGGER_H
#define RD_CPP_CORE_LOGGER_H

#include "thirdparty.hpp"
#include "util/core_util.h"

#include <string>
#include <exception>
#include <iostream>
#include <thread>
#include <cstdarg>
#include <mutex>

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
			const char *const format_ptr = format.data();

			va_list arg_list;
			va_start(arg_list, format);

			va_list va_copy;
			va_copy(va_copy, arg_list);
			const int len = std::vsnprintf(nullptr, 0, format_ptr, va_copy);
			va_end(va_copy);

			std::string buf;
			buf.resize(len + 1);
			std::vsnprintf(&buf[0], buf.size(), format_ptr, arg_list);
			va_end(arg_list);
			return buf;
		}

		static std::mutex lock;
		std::ostream &out;
	public:
		explicit Logger(std::ostream &out = std::cerr) : out(out) {}

		template<typename... Args>
		void log(std::exception const *e, LogLevel level, string_view format, Args const &... args) const {
			if (level >= minimum_level_to_log) {
				std::lock_guard<std::mutex> guard(Logger::lock);
				out << to_string(level) << " | " << to_string(std::this_thread::get_id()) << " | ";
				out << string_format(format, args...);
				if (e) {
					out << " | " << e->what();
				}
				out << std::endl;
			}
		}

		template<typename... Args>
		void trace(const std::exception *e, string_view msg, Args const &... args) const {
			log(e, LogLevel::Trace, msg, args...);
		}

		template<typename... Args>
		void trace(string_view msg, Args const &... args) const {
			log(nullptr, LogLevel::Trace, msg, args...);
		}

		template<typename... Args>
		void debug(const std::exception *e, string_view msg, Args const &... args) const {
			log(LogLevel::Debug, msg, args...);
		}

		template<typename... Args>
		void debug(string_view msg, Args const &... args) const {
			log(nullptr, LogLevel::Debug, msg, args...);
		}

		template<typename... Args>
		void info(const std::exception *e, string_view msg, Args const &... args) const {
			log(e, LogLevel::Info, msg, args...);
		}

		template<typename... Args>
		void info(string_view msg, Args const &... args) const {
			log(nullptr, LogLevel::Info, msg, args...);
		}

		template<typename... Args>
		void warn(const std::exception *e, string_view msg, Args const &... args) const {
			log(e, LogLevel::Warn, msg, args...);
		}

		template<typename... Args>
		void warn(string_view msg, Args const &... args) const {
			log(nullptr, LogLevel::Warn, msg, args...);
		}

		template<typename... Args>
		void error(std::exception const *e, string_view msg, Args const &... args) const {
			log(e, LogLevel::Error, msg, args...);
		}

		template<typename... Args>
		void error(string_view msg, Args const &... args) const {
			log(nullptr, LogLevel::Error, msg, args...);
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

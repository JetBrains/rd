#ifndef RD_CPP_CORE_LOGGER_H
#define RD_CPP_CORE_LOGGER_H

#include "thirdparty.hpp"

#include <string>
#include <exception>

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
	public:
		void log(LogLevel level, string_view format, va_list args, std::exception const *e = nullptr) const;

		void trace(const std::exception *e, string_view msg, ...) const;

		void trace(string_view msg, ...) const;

		void debug(const std::exception *e, string_view msg, ...) const;

		void debug(string_view msg, ...) const;

		void info(const std::exception *e, string_view msg, ...) const;

		void info(string_view msg, ...) const;

		void warn(const std::exception *e, string_view msg, ...) const;

		void warn(string_view msg, ...) const;

		void error(std::exception const *e, string_view msg, ...) const;

		void error(string_view msg, ...) const;
	};

	/*class SwitchLogger : public	Logger {

	public:
		SwitchLogger(const std::string &category);

		void log(LogLevel level, std::string message, std::exception const &e) override;

		bool is_enabled(LogLevel level) override;
	};*/

	//SwitchLogger get_logger(std::string category);

	template<typename F>
	void catch_(std::string comment, F &&action) {
		try {
			action();
		}
		catch (std::exception const &e) {
			std::string sfx = comment + std::string(
					e.what());
			Logger().error("Caught exception: %s", sfx.c_str());
		}
	}

	template<typename F>
	void catch_(F &&action) {
		catch_({}, std::forward<F>(action));
	}

}

#endif //RD_CPP_CORE_LOGGER_H

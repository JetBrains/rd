#ifndef RD_CPP_CORE_LOGGER_H
#define RD_CPP_CORE_LOGGER_H

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

	std::string to_string(LogLevel level);

	class Logger {
	public:
		/*virtual */void
		log(LogLevel level, const std::string &message, std::exception const *e = nullptr)/* = 0;*/const;

		void trace(std::string const &msg, std::exception const *e = nullptr) const;

		void debug(std::string const &msg, std::exception const *e = nullptr) const;

		void info(std::string const &msg, std::exception const *e = nullptr) const;

		void warn(std::string const &msg, std::exception const *e = nullptr) const;

		void error(std::string const &msg, std::exception const *e = nullptr) const;

		//    virtual bool is_enabled(LogLevel level) = 0;
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
			Logger().error("Caught exception:" + sfx);
		}
	}

	template<typename F>
	void catch_(F &&action) {
		catch_({}, std::forward<F>(action));
	}

}

#endif //RD_CPP_CORE_LOGGER_H

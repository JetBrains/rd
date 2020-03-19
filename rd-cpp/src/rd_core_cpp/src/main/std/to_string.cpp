#include "to_string.h"

namespace rd {
	namespace detail {
		std::string to_string(std::string const &val) {
			return val;
		}

		std::string detail::to_string(const char *val) {
			return val;
		}

		std::string detail::to_string(std::wstring const &val) {
			return std::string(val.begin(), val.end());
		}

		std::string detail::to_string(std::thread::id const &id) {
			std::ostringstream ss;
			ss << id;
			return ss.str();
		}

		std::string detail::to_string(std::exception const &e) {
			return std::string(e.what());
		}

		std::string detail::to_string(std::future_status const &status) {
			switch (status) {
				case std::future_status::ready:
					return "ready";
				case std::future_status::timeout:
					return "timeout";
				case std::future_status::deferred:
					return "deferred";
				default:
					return "unknown";
			}
		}

		std::wstring to_wstring(std::string const &s) {
			return std::wstring(s.begin(), s.end());
		}
	}
}



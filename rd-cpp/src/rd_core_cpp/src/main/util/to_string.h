#ifndef RD_CPP_TO_STRING_H
#define RD_CPP_TO_STRING_H

#include <string>
#include <type_traits>
#include <thread>
#include <sstream>
#include <vector>
#include <atomic>

#include "thirdparty.hpp"

namespace rd {
	namespace detail {
		using std::to_string;

		/*template<typename T>
		std::string to_string(T const &val);*/

		inline std::string to_string(std::string const &val) {
			return val;
		}

//		template<>
		inline std::string to_string(std::wstring const &val) {
			return std::string(val.begin(), val.end());
		}

		template<typename T>
		inline std::string to_string(optional<T> const &val) {
			if (val.has_value()) {
				return to_string(*val);
			} else {
				return "nullopt";
			}
		}

		inline std::string to_string(std::thread::id const &id) {
			std::ostringstream ss;
			ss << id;
			return ss.str();
		}

		inline std::string to_string(std::exception const &e) {
			return std::string(e.what());
		}

		template<typename T>
		inline std::string to_string(std::chrono::duration<int64_t, T> const &time) {
			return std::to_string(time.count());
		}

		template<typename T>
		inline std::string to_string(std::atomic<T> const &value) {
			return to_string(value.load());
		}

		template<typename F, typename S>
		inline std::string to_string(const std::pair<F, S> p) {
			return "(" + to_string(p.first) + ", " + to_string(p.second) + ")";
		}

		template<typename F, typename S>
		inline std::string to_string(const std::pair<F, S *> p) {
			return "(" + to_string(p.first) + ", " + to_string(*p.second) + ")";
		}

		template<typename F, typename S>
		inline std::string to_string(const std::pair<F *, S *> p) {
			return "(" + to_string(*p.first) + ", " + to_string(*p.second) + ")";
		}


		template<typename T>
		std::string to_string(std::vector<T> const &v) {
			std::string res = "[";
			for (const auto &item : v) {
				res += to_string(item);
				res += ",";
			}
			res += "]";
			return res;
		}

		template<class T>
		std::string as_string(T const &t) {
			return to_string(t);
		}

		using std::to_wstring;

		inline std::wstring to_wstring(std::string const &s) {
			return std::wstring(s.begin(), s.end());
		}

		template<class T>
		std::wstring as_wstring(T const &t) {
			return to_wstring(t);
		}
	}

	template<typename T>
	std::string to_string(T const &val) {
		return detail::as_string(val);
	}

	template<typename T>
	std::wstring to_wstring(T const &val) {
		return detail::as_wstring(val);
	}
}


#endif //RD_CPP_TO_STRING_H

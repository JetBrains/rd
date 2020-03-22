#ifndef RD_CPP_TO_STRING_H
#define RD_CPP_TO_STRING_H

#include <string>
#include <type_traits>
#include <thread>
#include <sstream>
#include <vector>
#include <atomic>
#include <future>


#include "thirdparty.hpp"

namespace rd {
	namespace detail {
		using std::to_string;

		std::string to_string(std::string const &val);

		std::string to_string(char const val[]);

		std::string to_string(std::wstring const &val);

		std::string to_string(std::thread::id const &id);

		std::string to_string(std::exception const &e);

		std::string to_string(std::future_status const &status);

		template<typename Rep, typename Period>
		inline std::string to_string(std::chrono::duration<Rep, Period> const &time) {
			return std::to_string(std::chrono::duration_cast<std::chrono::milliseconds>(time).count()) + "ms";
		}

		template<typename T>
		inline std::string to_string(T const *val) {
			return val ? to_string(*val) : "nullptr";
		}

		template<typename T>
		inline std::string to_string(std::atomic<T> const &value) {
			return to_string(value.load());
		}

		template<typename T>
		inline std::string to_string(optional<T> const &val) {
			if (val.has_value()) {
				return to_string(*val);
			} else {
				return "nullopt";
			}
		}

		template<typename F, typename S>
		inline std::string to_string(const std::pair<F, S> p) {
			return "(" + to_string(p.first) + ", " + to_string(p.second) + ")";
		}

		template<template<class, class> class C, typename T, typename A>
		std::string to_string(C<T, A> const &v) {
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

		std::wstring to_wstring(std::string const &s);

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

#ifndef RD_CPP_CORE_CPP_UTIL_H
#define RD_CPP_CORE_CPP_UTIL_H

#include "erase_if.h"
#include "gen_util.h"
#include "overloaded.h"
#include "shared_function.h"
#include "wrapper.h"
#include "Void.h"

#include "thirdparty.hpp"

#include <memory>
#include <string>
#include <thread>
#include <atomic>
#include <iostream>
#include <sstream>
#include <cassert>

#define RD_ASSERT_MSG(expr, msg) if(!(expr)){std::cerr<<std::endl<<(msg)<<std::endl;assert(expr);}
#define RD_ASSERT_THROW_MSG(expr, msg) if(!(expr)){std::cerr<<std::endl<<(msg)<<std::endl;throw std::runtime_error(msg);}

namespace rd {
	namespace wrapper {
		template<typename T>
		struct TransparentKeyEqual {
			using is_transparent = void;

			bool operator()(T const &val_l, T const &val_r) const {
				return val_l == val_r;
			}

			bool operator()(Wrapper<T> const &ptr_l, Wrapper<T> const &ptr_r) const {
				return ptr_l == ptr_r;
			}

			bool operator()(T const *val_l, T const *val_r) const {
				return *val_l == *val_r;
			}

			bool operator()(T const &val_r, Wrapper<T> const &ptr_l) const {
				return *ptr_l == val_r;
			}

			bool operator()(T const &val_l, T const *ptr_r) const {
				return val_l == *ptr_r;
			}

			bool operator()(Wrapper<T> const &val_l, T const *ptr_r) const {
				return *val_l == *ptr_r;
			}
		};

		template<typename T>
		struct TransparentHash {
			using is_transparent = void;
			using transparent_key_equal = std::equal_to<>;

			size_t operator()(T const &val) const noexcept {
				return std::hash<T>()(val);
			}

			size_t operator()(Wrapper<T> const &ptr) const noexcept {
				return std::hash<Wrapper<T>>()(ptr);
			}

			size_t operator()(T const *val) const noexcept {
				return std::hash<T>()(*val);
			}
		};
	}

//region to_string

/*
    template<typename T>
    std::string to_string(T const &val) {
        return "";
    }
*/

	template<typename T>
	typename std::enable_if_t<std::is_arithmetic<T>::value, std::string> to_string(T const &val) {
		return std::to_string(val);
	}

	template<typename T>
	typename std::enable_if_t<!std::is_arithmetic<T>::value, std::string> to_string(T const &val) {
		return "";
	}

	template<>
	inline std::string to_string<std::string>(std::string const &val) {
		return val;
	}

	template<>
	inline std::string to_string<std::wstring>(std::wstring const &val) {
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
	std::string to_string(std::chrono::duration<int64_t, T> const &time) {
		return std::to_string(time.count());
	}

	template<typename T>
	std::string to_string(Wrapper<T> const &value) {
		return to_string(*value);
	}

	template<typename T>
	std::string to_string(std::atomic<T> const &value) {
		return to_string(value.load());
	}
	//endregion

	inline std::wstring to_wstring(std::string const &s) {
		return std::wstring(s.begin(), s.end());
	}

	template<typename F, typename S>
	std::string to_string(const std::pair<F, S> p) {
		return "(" + to_string(p.first) + ", " + to_string(p.second) + ")";
	}

	template<typename F, typename S>
	std::string to_string(const std::pair<F, S *> p) {
		return "(" + to_string(p.first) + ", " + to_string(*p.second) + ")";
	}

	template<typename F, typename S>
	std::string to_string(const std::pair<F *, S *> p) {
		return "(" + to_string(*p.first) + ", " + to_string(*p.second) + ")";
	}
}

#endif //RD_CPP_CORE_CPP_UTIL_H

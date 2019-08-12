#ifndef RD_CPP_GEN_UTIL_H
#define RD_CPP_GEN_UTIL_H

#include "hash.h"

#include <cstdlib>
#include <vector>

namespace rd {
	template<typename T>
	size_t contentHashCode(std::vector<T> const &list) noexcept {
		size_t __r = 0;
		for (auto const &e : list) {
			__r = __r * 31 + rd::hash<T>()(e);
		}
		return __r;
		//todo faster for integrals
	}

	template<typename T>
	size_t contentDeepHashCode(T const &value) noexcept {
		return rd::hash<T>()(value);
	}

	template<typename T>
	typename std::enable_if<std::is_integral<T>::value, size_t>::type
	contentDeepHashCode(std::vector<T> const &value) noexcept {
		return contentHashCode(value);
	}

	template<typename T>
	typename std::enable_if<!std::is_integral<T>::value, size_t>::type
	contentDeepHashCode(std::vector<T> const &value) noexcept {
		size_t result = 1;
		for (auto const &x : value) {
			result = 31 * result + contentDeepHashCode(x);
		}
		return result;
	}

}

#endif //RD_CPP_GEN_UTIL_H

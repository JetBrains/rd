#ifndef RD_CPP_GEN_UTIL_H
#define RD_CPP_GEN_UTIL_H

#include "std/hash.h"
#include "std/allocator.h"

#include <cstdlib>

namespace rd {
	template<template<class, class> class C, typename T, typename A = allocator<T>>
	size_t contentHashCode(C<T, A> const &list) noexcept {
		size_t __r = 0;
		for (auto const &e : list) {
			__r = __r * 31 + hash<T>()(e);
		}
		return __r;
		//todo faster for integrals
	}

	template<typename T>
	size_t contentDeepHashCode(T const &value) noexcept {
		return rd::hash<T>()(value);
	}

	template<template<class, class> class C, typename T, typename A = allocator<T>>
	typename std::enable_if_t<std::is_integral<T>::value, size_t>
	contentDeepHashCode(C<T, A> const &value) noexcept {
		return contentHashCode(value);
	}

	template<template<class, class> class C, typename T, typename A = allocator<T>>
	typename std::enable_if_t<!std::is_integral<T>::value, size_t>
	contentDeepHashCode(C<T, A> const &value) noexcept {
		size_t result = 1;
		for (auto const &x : value) {
			result = 31 * result + contentDeepHashCode<T>(x);
		}
		return result;
	}
}

#endif //RD_CPP_GEN_UTIL_H

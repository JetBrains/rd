//
// Created by jetbrains on 3/23/2019.
//

#ifndef RD_CPP_FRAMEWORK_TRAITS_H
#define RD_CPP_FRAMEWORK_TRAITS_H

// #include "SerializationCtx.h"
// #include "Buffer.h"

#include <utility>
#include <type_traits>

namespace rd {
	namespace util {
		template<typename S, typename T = decltype((S::read(std::declval<rd::SerializationCtx const &>(), std::declval<rd::Buffer const &>())))>
		using read_t = T;

		static_assert(util::is_same_v<std::wstring, read_t<Polymorphic<std::wstring>>>, " ");
	}
}

#endif //RD_CPP_FRAMEWORK_TRAITS_H

//
// Created by jetbrains on 12.02.2019.
//

#ifndef RD_CPP_TRAITS_H
#define RD_CPP_TRAITS_H

#include "Void.h"

#include <type_traits>

namespace rd {
	template<typename T>
	using is_void = typename std::enable_if<std::is_same<T, Void>::value>;
}

#endif //RD_CPP_TRAITS_H

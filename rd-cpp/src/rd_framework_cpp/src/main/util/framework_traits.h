//
// Created by jetbrains on 3/23/2019.
//

#ifndef RD_CPP_FRAMEWORK_TRAITS_H
#define RD_CPP_FRAMEWORK_TRAITS_H

#include "SerializationCtx.h"
#include "Buffer.h"

namespace rd {
	namespace util {
		template<typename S>
		using read_t = decltype(S::read(std::declval<SerializationCtx>(), std::declval<Buffer>()));
	}
}

#endif //RD_CPP_FRAMEWORK_TRAITS_H

//
// Created by jetbrains on 12.11.2018.
//

#ifndef RD_CPP_ARRAYSERIALIZER_H
#define RD_CPP_ARRAYSERIALIZER_H

#include "SerializationCtx.h"
#include "framework_traits.h"

#include <vector>

namespace rd {
	template<typename S, typename T = util::read_t<S>>
	class ArraySerializer {
	public:
		static std::vector<value_or_wrapper<T>> read(SerializationCtx const &ctx, Buffer const &buffer) {
			return buffer.readArray<T>([&] { return S::read(ctx, buffer); });
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, std::vector<value_or_wrapper<T>> const &value) {
			buffer.writeArray<T>(value, [&](T const &inner_value) { S::write(ctx, buffer, inner_value); });
		}
	};
}

#endif //RD_CPP_ARRAYSERIALIZER_H

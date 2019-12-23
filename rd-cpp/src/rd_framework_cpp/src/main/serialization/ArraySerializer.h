#ifndef RD_CPP_ARRAYSERIALIZER_H
#define RD_CPP_ARRAYSERIALIZER_H

#include "serialization/SerializationCtx.h"
#include "framework_traits.h"

#include <vector>

namespace rd {
	template<typename S, typename T = typename util::read_t<S>>
	class ArraySerializer {
	public:
		static std::vector<value_or_wrapper<T>> read(SerializationCtx  &ctx, Buffer &buffer) {
			return buffer.read_array<T>([&] { return S::read(ctx, buffer); });
		}

		static void write(SerializationCtx  &ctx, Buffer &buffer, std::vector<value_or_wrapper<T>> const &value) {
			buffer.write_array<T>(value, [&](T const &inner_value) { S::write(ctx, buffer, inner_value); });
		}
	};
}

#endif //RD_CPP_ARRAYSERIALIZER_H

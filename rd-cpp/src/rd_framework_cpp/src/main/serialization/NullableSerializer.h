//
// Created by jetbrains on 12.11.2018.
//

#ifndef RD_CPP_NULLABLESERIALIZER_H
#define RD_CPP_NULLABLESERIALIZER_H

#include "Polymorphic.h"
#include "AbstractPolymorphic.h"
#include "wrapper.h"
#include "framework_traits.h"

#include <type_traits>

namespace rd {
	template<typename S, typename T = util::read_t<S>,
	        typename = void>
	class NullableSerializer {
	public:
		static opt_or_wrapper<T> read(SerializationCtx const &ctx, Buffer const &buffer) {
			return buffer.readNullable<T>([&]() { return S::read(ctx, buffer); });
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, tl::optional<T> const &value) {
			buffer.writeNullable<T>(value, [&](T const &inner_value) { S::write(ctx, buffer, inner_value); });
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, Wrapper<T> const &value) {
			buffer.writeNullable<T>(value, [&](T const &inner_value) { S::write(ctx, buffer, inner_value); });
		}
	};

	template<typename S, typename W>
	class NullableSerializer<S, W, std::enable_if_t<is_wrapper_v<util::read_t<S>>>> {
//		using W = decltype(S::read(std::declval<SerializationCtx>(), std::declval<Buffer>()));
		using T = typename W::type;
	public:
		static Wrapper<T> read(SerializationCtx const &ctx, Buffer const &buffer) {
			bool nullable = !buffer.readBool();
			if (nullable) {
				return {};
			}
			return S::read(ctx, buffer);
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, Wrapper<T> const &value) {
			buffer.writeNullable<T>(value, [&](T const &inner_value) { S::write(ctx, buffer, inner_value); });
		}
	};
}


#endif //RD_CPP_NULLABLESERIALIZER_H

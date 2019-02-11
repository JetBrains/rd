//
// Created by jetbrains on 12.11.2018.
//

#ifndef RD_CPP_NULLABLESERIALIZER_H
#define RD_CPP_NULLABLESERIALIZER_H

#include "SerializationCtx.h"
#include "Polymorphic.h"

#include <type_traits>

namespace rd {
	template<typename S, typename T = decltype(S::read(std::declval<SerializationCtx>(), std::declval<Buffer>()))>
	class NullableSerializer {
	public:
		static opt_or_wrapper<T> read(SerializationCtx const &ctx, Buffer const &buffer) {
			return buffer.readNullable<T>([&]() -> T { return S::read(ctx, buffer); });
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, opt_or_wrapper<T> const &value) {
			buffer.writeNullable<T>(value, [&](T const &inner_value) { S::write(ctx, buffer, inner_value); });
		}
	};

	template<typename T>
	class NullableSerializer<AbstractPolymorphic<T>> {
		using S = AbstractPolymorphic<T>;
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

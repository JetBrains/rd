//
// Created by jetbrains on 3/2/2019.
//

#ifndef RD_CPP_INTERNEDSERIALIZER_H
#define RD_CPP_INTERNEDSERIALIZER_H

#include "SerializationCtx.h"
#include "Polymorphic.h"
#include "framework_traits.h"

namespace rd {
	template<typename S, util::hash_t InternKey, typename T = typename util::read_t<S>>
	class InternedSerializer {
	public:
		static Wrapper<T> read(SerializationCtx const &ctx, Buffer const &buffer) {
			return ctx.readInterned<T, InternKey>(buffer, [&](SerializationCtx const &, Buffer const &) {
				return S::read(ctx, buffer);
			});
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, Wrapper<T> const &value) {
			ctx.writeInterned<T, InternKey>(buffer, value, [&](SerializationCtx const &, Buffer const &, T const & inner_value) -> void {
				S::write(ctx, buffer, inner_value);
			});
		}
	};
}
#endif //RD_CPP_INTERNEDSERIALIZER_H

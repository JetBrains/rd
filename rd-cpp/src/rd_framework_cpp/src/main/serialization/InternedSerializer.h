//
// Created by jetbrains on 3/2/2019.
//

#ifndef RD_CPP_INTERNEDSERIALIZER_H
#define RD_CPP_INTERNEDSERIALIZER_H

#include "SerializationCtx.h"
#include "Polymorphic.h"

namespace rd {
	template<typename S, RdId::hash_t InternKey, typename T = decltype(S::read(std::declval<SerializationCtx>(), std::declval<Buffer>()))>
	class InternedSerializer {
	public:
		static T read(SerializationCtx const &ctx, Buffer const &buffer) {
			return ctx.readInterned<T, InternKey>(buffer, [&](SerializationCtx const &, Buffer const &) {
				return S::read(ctx, buffer);
			});
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) {
			ctx.writeInterned<T, InternKey>(buffer, value, [&](SerializationCtx const &, Buffer const &, T const & inner_value) {
				S::write(ctx, buffer, inner_value);
			});
		}
	};
}
#endif //RD_CPP_INTERNEDSERIALIZER_H

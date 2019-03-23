//
// Created by jetbrains on 3/10/2019.
//

#ifndef RD_CPP_ANYSERIALIZER_H
#define RD_CPP_ANYSERIALIZER_H

#include "SerializationCtx.h"
#include "RdAny.h"

#include "optional.hpp"

namespace rd {
	//region predeclared

	class Buffer;
	//endregion

	class InternedAnySerializer {
	public:
		static tl::optional<InternedAny> read(SerializationCtx const &ctx, Buffer const &buffer) {
			return ctx.get_serializers().readAny(ctx, buffer);
		}

		template<typename T>
		static void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) {
			ctx.get_serializers().writePolymorphicNullable(ctx, buffer, value);
		}
	};
}

#endif //RD_CPP_ANYSERIALIZER_H

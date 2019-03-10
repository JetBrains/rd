//
// Created by jetbrains on 3/10/2019.
//

#ifndef RD_CPP_ANYSERIALIZER_H
#define RD_CPP_ANYSERIALIZER_H

#include "SerializationCtx.h"
#include "Buffer.h"

namespace rd {
	class AnySerializer {
	public:
		static tl::optional<RdAny> read(SerializationCtx const &ctx, Buffer const &buffer) {
			return ctx.serializers->readAny(ctx, buffer);
		}
	};
}

#endif //RD_CPP_ANYSERIALIZER_H

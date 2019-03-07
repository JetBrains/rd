#ifndef RD_CPP_ABSTRACTPOLYMORPHIC_H
#define RD_CPP_ABSTRACTPOLYMORPHIC_H

#include "Polymorphic.h"
#include "wrapper.h"

namespace rd {
	template<typename T>
	class AbstractPolymorphic {
	public:
		static value_or_wrapper <T> read(SerializationCtx const &ctx, Buffer const &buffer) {
			return ctx.serializers->readPolymorphicNullable<T>(ctx, buffer);
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) {
			ctx.serializers->writePolymorphicNullable(ctx, buffer, value);
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, Wrapper <T> const &value) {
			ctx.serializers->writePolymorphicNullable(ctx, buffer, *value);
		}
	};
}

#endif //RD_CPP_ABSTRACTPOLYMORPHIC_H

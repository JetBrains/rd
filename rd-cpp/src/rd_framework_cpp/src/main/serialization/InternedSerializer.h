//
// Created by jetbrains on 3/2/2019.
//

#ifndef RD_CPP_INTERNEDSERIALIZER_H
#define RD_CPP_INTERNEDSERIALIZER_H

#include "SerializationCtx.h"
#include "Polymorphic.h"

namespace rd {
	template<typename S, typename T = decltype(S::read(std::declval<SerializationCtx>(), std::declval<Buffer>()))>
	class InternedSerializer {
	public:
		static T read(SerializationCtx const &ctx, Buffer const &buffer) {
/*
            return ctx.readInterned(buffer, intern_key, [&](SerializationCtx const &, Buffer const &){
                return S::read(buffer, ctx);
            });
*/
		}

		static void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) {
/*
            ctx.writeInterned(buffer, value, intern_key, [&](){
                S::write(buffer, ctx, value);
            });
*/
		}
	};
}
#endif //RD_CPP_INTERNEDSERIALIZER_H

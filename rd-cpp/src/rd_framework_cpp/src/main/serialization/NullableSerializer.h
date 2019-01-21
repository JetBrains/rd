//
// Created by jetbrains on 12.11.2018.
//

#ifndef RD_CPP_NULLABLESERIALIZER_H
#define RD_CPP_NULLABLESERIALIZER_H

#include "SerializationCtx.h"

#include "optional.hpp"

#include <type_traits>

template<typename S, typename T = decltype(S::read(std::declval<SerializationCtx>(), std::declval<Buffer>()))>
class NullableSerializer {
public:
    static tl::optional<T> read(SerializationCtx const &ctx, Buffer const &buffer) {
        return buffer.readNullable<T>([&]() -> T { return S::read(ctx, buffer); });
    }

    static void write(SerializationCtx const &ctx, Buffer const &buffer, tl::optional<T> const &value) {
        buffer.writeNullable<T>(value, [&](T const &inner_value) { S::write(ctx, buffer, inner_value); });
    }
};


#endif //RD_CPP_NULLABLESERIALIZER_H

//
// Created by jetbrains on 12.11.2018.
//

#ifndef RD_CPP_NULLABLESERIALIZER_H
#define RD_CPP_NULLABLESERIALIZER_H

#include <optional>
#include <type_traits>

#include "SerializationCtx.h"

template<typename S, typename T = decltype(S::read(std::declval<SerializationCtx>(), std::declval<Buffer>()))>
class NullableSerializer {
public:
    static std::optional<T> read(SerializationCtx const &ctx, Buffer const &buffer) {
        return buffer.readNullable<T>([&]() -> T { return S::read(ctx, buffer); });
    }

    static void write(SerializationCtx const &ctx, Buffer const &buffer, std::optional<T> const &value) {
        buffer.writeNullable<T>(value, [&](T const &inner_value) { S::write(ctx, buffer, inner_value); });
    }
};


#endif //RD_CPP_NULLABLESERIALIZER_H

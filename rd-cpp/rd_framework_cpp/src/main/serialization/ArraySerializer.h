//
// Created by jetbrains on 12.11.2018.
//

#ifndef RD_CPP_ARRAYSERIALIZER_H
#define RD_CPP_ARRAYSERIALIZER_H

#include <vector>

#include "SerializationCtx.h"
#include "Polymorphic.h"

template<typename S, typename T = decltype(S::read(std::declval<SerializationCtx>(), std::declval<Buffer>()))>
class ArraySerializer {
public:
    static std::vector<T> read(SerializationCtx const &ctx, Buffer const &buffer) {
        return buffer.readArray<T>([&]() { return S::read(ctx, buffer); });
    }

    static void write(SerializationCtx const &ctx, Buffer const &buffer, std::vector<T> const &value) {
        buffer.writeArray<T>(value, [&](T const &inner_value) { S::write(ctx, buffer, inner_value); });
    }
};

#endif //RD_CPP_ARRAYSERIALIZER_H

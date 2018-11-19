//
// Created by jetbrains on 30.07.2018.
//

#ifndef RD_CPP_POLYMORPHIC_H
#define RD_CPP_POLYMORPHIC_H

#include "ISerializer.h"
#include "RdReactiveBase.h"

#include <type_traits>


template<typename T, typename R = void>
class Polymorphic/* : public ISerializer<T>*/ {
public:
    static T read(SerializationCtx const &ctx, Buffer const &buffer) {
        return ctx.serializers->readPolymorphic<T>(ctx, buffer);
    }

    static void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) {
        ctx.serializers->writePolymorphic(ctx, buffer, value);
    }

};

template<typename T>
class Polymorphic<T, typename std::enable_if_t<std::is_integral_v<T> > > {
public:
    static T read(SerializationCtx const &ctx, Buffer const &buffer) {
        return buffer.read_pod<T>();
    }

    static void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) {
        buffer.write_pod<T>(value);
    }

};

//class Polymorphic<int, void>;

template<typename T>
class Polymorphic<std::vector<T>> {
public:
    static std::vector<T> read(SerializationCtx const &ctx, Buffer const &buffer) {
        return buffer.readArray<T>();
    }

    static void write(SerializationCtx const &ctx, Buffer const &buffer, std::vector<T> const &value) {
        buffer.writeArray<T>(value);
    }
};

template<>
class Polymorphic<std::string> {
public:
    static std::string read(SerializationCtx const &ctx, Buffer const &buffer) {
        return buffer.readString();
    }

    static void write(SerializationCtx const &ctx, Buffer const &buffer, std::string const &value) {
        buffer.writeString(value);
    }
};

template<>
class Polymorphic<void *> {
public:
    static void *read(SerializationCtx const &ctx, Buffer const &buffer) {
        return nullptr;
    }

    static void write(SerializationCtx const &ctx, Buffer const &buffer, void *const &value) {}
};

template<typename T>
class Polymorphic<T, typename std::enable_if_t<std::is_base_of_v<RdReactiveBase, T> > > {
public:
    static T read(SerializationCtx const &ctx, Buffer const &buffer) {
        return T::read(ctx, buffer);
    }

    static void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) {
        value.write(ctx, buffer);
    }
};

template<typename T>
class Polymorphic<T, typename std::enable_if_t<std::is_enum_v<T> > > {
public:
    static T read(SerializationCtx const &ctx, Buffer const &buffer) {
        return buffer.readEnum<T>();
    }

    static void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) {
        buffer.writeEnum<T>(value);
    }
};

template<typename T>
class Polymorphic<tl::optional<T>> {
public:
    static tl::optional<T> read(SerializationCtx const &ctx, Buffer const &buffer) {
        return buffer.readNullable<T>([&ctx, &buffer]() {
            return Polymorphic<T>::read(ctx, buffer);
        });
    }

    static void write(SerializationCtx const &ctx, Buffer const &buffer, tl::optional<T> const &value) {
        buffer.writeNullable<T>(value, [&ctx, &buffer](T const &v) {
            Polymorphic<T>::write(ctx, buffer, v);
        });
    }
};

#endif //RD_CPP_POLYMORPHIC_H

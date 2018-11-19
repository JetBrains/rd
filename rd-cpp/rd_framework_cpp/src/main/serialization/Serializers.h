//
// Created by jetbrains on 25.07.2018.
//

#ifndef RD_CPP_SERIALIZERS_H
#define RD_CPP_SERIALIZERS_H

#include "custom_type_traits.h"

#include "RdId.h"
#include "ISerializable.h"
#include "Identities.h"
#include "demangle.h"

#include <utility>
//#include <any>
#include <iostream>
#include <unordered_map>

class SerializationCtx;

class Serializers {
public:
    mutable std::unordered_map<RdId, std::function<std::unique_ptr<ISerializable>(SerializationCtx const &,
                                                                                  Buffer const &)>> readers;

    template<typename T, typename = typename std::enable_if<std::is_base_of<ISerializable, T>::value>::type>
    void registry() const {
        std::string type_name = demangle<T>();
        hash_t h = getPlatformIndependentHash(type_name);
        RdId id(h);

//        Protocol::initializationLogger.trace("Registering type " + type_name + ", id = " + id.toString());
//todo uncomment

        MY_ASSERT_MSG(readers.count(id) == 0, "Can't register " + type_name + " with id: " + id.toString());

        readers[id] = [](SerializationCtx const &ctx,
                         Buffer const &buffer) -> std::unique_ptr<ISerializable> {
            return std::make_unique<T>(T::read(ctx, buffer));
        };
    }

    template<typename T>
    T readPolymorphic(SerializationCtx const &ctx, Buffer const &stream) const {
        RdId id = RdId::read(stream);
        int32_t size = stream.read_pod<int32_t>();
        stream.check_available(size);

        if (readers.count(id) == 0) {
//            std::cerr << std::endl << ' ' << id.get_hash() << '\n';
            throw std::invalid_argument("no reader");
        }
        auto const &reader = readers.at(id);
        std::unique_ptr<ISerializable> ptr = reader(ctx, stream);
        T res = std::move(*dynamic_cast<T *>(ptr.get()));
        return res;
    }

    /*template<typename T>
    void registry(std::function<T(SerializationCtx const &, Buffer const &)> reader) const {
        std::string type_name = demangle<T>();
        hash_t h = getPlatformIndependentHash(type_name);
        std::cerr << "registry: " << std::string(type_name) << " with hash: " << h << std::endl;
//        std::cout << std::endl << typeid(T).name() << std::endl;
        RdId id(h);
//        Protocol.initializationLogger.trace { "Registering type ${t.simpleName}, id = $id" }

        auto real_reader = [reader](SerializationCtx const &ctx,
                                    Buffer const &buffer) -> std::unique_ptr<ISerializable> {
            T object = reader(ctx, buffer);
            return std::make_unique<T>(std::move(object));
        };
        readers[id] = std::move(real_reader);
    }*/

    template<typename T>
    void writePolymorphic(SerializationCtx const &ctx, Buffer const &stream, const T &value) const {
        std::string type_name = demangle<T>();
        hash_t h = getPlatformIndependentHash(type_name);
        std::cerr << "write: " << type_name << " with hash: " << h << std::endl;
        RdId(h).write(stream);


        int32_t lengthTagPosition = stream.get_position();
        stream.write_pod<int32_t>(0);
        int32_t objectStartPosition = stream.get_position();
        value.write(ctx, stream);
        int32_t objectEndPosition = stream.get_position();
        stream.set_position(lengthTagPosition);
        stream.write_pod<int32_t>(objectEndPosition - objectStartPosition);
        stream.set_position(objectEndPosition);
    }
};


#endif //RD_CPP_SERIALIZERS_H

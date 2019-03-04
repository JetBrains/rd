//
// Created by jetbrains on 25.07.2018.
//

#ifndef RD_CPP_SERIALIZERS_H
#define RD_CPP_SERIALIZERS_H

#include "RdId.h"
#include "ISerializable.h"
#include "Identities.h"
#include "IUnknownInstance.h"

#include <utility>
#include <iostream>
#include <unordered_map>

namespace rd {
	//region predeclared

	class SerializationCtx;
	//endregion

	class Serializers {
	private:

		static RdId real_rd_id(IUnknownInstance const &value);

		static RdId real_rd_id(IPolymorphicSerializable const &value);

	public:
		mutable std::unordered_map<RdId, std::function<std::unique_ptr<IPolymorphicSerializable>(SerializationCtx const &,
																					  Buffer const &)>> readers;

		template<typename T, typename = typename std::enable_if<std::is_base_of<IPolymorphicSerializable, T>::value>::type>
		void registry() const {
			std::string type_name = T().type_name();//todo don't call ctor
			RdId::hash_t h = getPlatformIndependentHash(type_name);
			RdId id(h);

			//        Protocol::initializationLogger.trace("Registering type " + type_name + ", id = " + id.toString());
			//todo uncomment

			MY_ASSERT_MSG(readers.count(id) == 0, "Can't register " + type_name + " with id: " + id.toString());

			readers[id] = [](SerializationCtx const &ctx, Buffer const &buffer) -> std::unique_ptr<IPolymorphicSerializable> {
				return std::make_unique<T>(T::read(ctx, buffer));
			};
		}

		template<typename T>
		std::unique_ptr<T> readPolymorphicNullable(SerializationCtx const &ctx, Buffer const &stream) const {
			RdId id = RdId::read(stream);
			if (id.isNull()) {
				return nullptr;
			}
			int32_t size = stream.read_integral<int32_t>();
			stream.check_available(size);

			if (readers.count(id) == 0) {
				//            std::cerr << std::endl << ' ' << id.get_hash() << '\n';
				throw std::invalid_argument("no reader");
			}
			auto const &reader = readers.at(id);
			std::unique_ptr<IPolymorphicSerializable> ptr = reader(ctx, stream);
			return std::unique_ptr<T>(dynamic_cast<T *>(ptr.release()));
			//todo change the way of dynamic_pointer_cast
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


		template<typename T, typename = typename std::enable_if<std::is_base_of<IPolymorphicSerializable, T>::value>::type>
		void writePolymorphicNullable(SerializationCtx const &ctx, Buffer const &stream, const T &value) const {
			real_rd_id(value).write(stream);

			int32_t length_tag_position = stream.get_position();
			stream.write_integral<int32_t>(0);
			int32_t object_start_position = stream.get_position();
			value.write(ctx, stream);
			int32_t object_end_position = stream.get_position();
			stream.set_position(length_tag_position);
			stream.write_integral<int32_t>(object_end_position - object_start_position);
			stream.set_position(object_end_position);
		}

		template<typename T>
		void writePolymorphic(SerializationCtx const &ctx, Buffer const &stream, const Wrapper<T> &value) const {
			writePolymorphicNullable(ctx, stream, *value);
		}
	};
}


#endif //RD_CPP_SERIALIZERS_H

//
// Created by jetbrains on 25.07.2018.
//

#ifndef RD_CPP_SERIALIZERS_H
#define RD_CPP_SERIALIZERS_H

#include "RdId.h"
#include "ISerializable.h"
#include "Identities.h"
#include "IUnknownInstance.h"
#include "hashing.h"
#include "RdAny.h"

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

		static RdId real_rd_id(std::wstring const &value);

		static void real_write(SerializationCtx const &ctx, Buffer const &buffer, IUnknownInstance const &value);

		static void real_write(SerializationCtx const &ctx, Buffer const &buffer, IPolymorphicSerializable const &value);

		static void real_write(SerializationCtx const &ctx, Buffer const &buffer, std::wstring const &value);


		mutable std::unordered_map<RdId, std::function<RdAny(SerializationCtx const &, Buffer const &)>> readers;
	public:
		Serializers();

		template<typename T, typename = typename std::enable_if<std::is_base_of<IPolymorphicSerializable, T>::value>::type>
		void registry() const;

		tl::optional<RdAny> readAny(SerializationCtx const &ctx, Buffer const &buffer) const;

		template<typename T>
		value_or_wrapper<T> readPolymorphicNullable(SerializationCtx const &ctx, Buffer const &buffer) const;

		template<typename T/*, typename = typename std::enable_if<std::is_base_of<IPolymorphicSerializable, T>::value>::type*/>
		void writePolymorphicNullable(SerializationCtx const &ctx, Buffer const &buffer, const T &value) const;

		template<typename T>
		void writePolymorphic(SerializationCtx const &ctx, Buffer const &stream, const Wrapper<T> &value) const;
	};
}

namespace rd {
	template<typename T, typename>
	void Serializers::registry() const {
		std::string type_name = T().type_name();//todo don't call ctor
		util::hash_t h = util::getPlatformIndependentHash(type_name);
		RdId id(h);

		//        Protocol::initializationLogger.trace("Registering type " + type_name + ", id = " + id.toString());
		//todo uncomment

		MY_ASSERT_MSG(readers.count(id) == 0, "Can't register " + type_name + " with id: " + id.toString());

		readers[id] = [](SerializationCtx const &ctx, Buffer const &buffer) -> Wrapper<IPolymorphicSerializable> {
			return std::make_shared<T>(T::read(ctx, buffer));
		};
	}

	template<typename T>
	value_or_wrapper<T> Serializers::readPolymorphicNullable(SerializationCtx const &ctx, Buffer const &buffer) const {
		tl::optional<RdAny> any = readAny(ctx, buffer);
		return any::get<T>(*(std::move(any)));
	}

	template<typename T/*, typename*/>
	void Serializers::writePolymorphicNullable(SerializationCtx const &ctx, Buffer const &buffer, const T &value) const {
		real_rd_id(value).write(buffer);

		int32_t length_tag_position = static_cast<int32_t>(buffer.get_position());
		buffer.write_integral<int32_t>(0);
		int32_t object_start_position = static_cast<int32_t>(buffer.get_position());
		real_write(ctx, buffer, value);
//		value.write(ctx, buffer);
		int32_t object_end_position = static_cast<int32_t>(buffer.get_position());
		buffer.set_position(static_cast<size_t>(length_tag_position));
		buffer.write_integral<int32_t>(object_end_position - object_start_position);
		buffer.set_position(static_cast<size_t>(object_end_position));
	}

	template<typename T>
	void Serializers::writePolymorphic(SerializationCtx const &ctx, Buffer const &stream, const Wrapper<T> &value) const {
		writePolymorphicNullable(ctx, stream, *value);
	}
}

#endif //RD_CPP_SERIALIZERS_H

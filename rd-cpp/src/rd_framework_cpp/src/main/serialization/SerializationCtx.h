//
// Created by jetbrains on 20.07.2018.
//

#ifndef RD_CPP_FRAMEWORK_SERIALIZATIONCTX_H
#define RD_CPP_FRAMEWORK_SERIALIZATIONCTX_H

#include "Buffer.h"
#include "RdId.h"

#include <unordered_map>
#include <functional>
#include <string>
#include <utility>
#include <regex>

namespace rd {
	//region predeclared

	class IProtocol;

	class Serializers;

	class InternRoot;

	class RdBindableBase;
	//endregion

	class SerializationCtx {
		Serializers const *serializers = nullptr;
	public:
		using roots_t = std::unordered_map<util::hash_t, InternRoot const *>;

		roots_t intern_roots{};

		//region ctor/dtor

		//    SerializationCtx() = delete;

		SerializationCtx(SerializationCtx &&other) = default;

		SerializationCtx &operator=(SerializationCtx &&other) = default;

//		explicit SerializationCtx(const Serializers *serializers = nullptr);

		explicit SerializationCtx(const Serializers *serializers, roots_t intern_roots = {});

		SerializationCtx withInternRootsHere(RdBindableBase const &owner, std::initializer_list<std::string> new_roots) const;

		//endregion

		template<typename T, util::hash_t InternKey>
		Wrapper<T> readInterned(Buffer const &buffer, std::function<T(SerializationCtx const &, Buffer const &)> readValueDelegate) const;

		template<typename T, util::hash_t InternKey, typename F,
						typename = typename std::enable_if_t<util::is_invocable<F, SerializationCtx, Buffer, Wrapper<T>>::value>
						>
		void writeInterned(Buffer const &buffer, Wrapper<T> const &value, F&& writeValueDelegate) const;

		Serializers const & get_serializers() const;
	};
}

#include "InternRoot.h"

namespace rd {
	template<typename T, util::hash_t InternKey>
	Wrapper<T> SerializationCtx::readInterned(Buffer const &buffer, std::function<T(const SerializationCtx &, const Buffer &)> readValueDelegate) const {
		auto it = intern_roots.find(InternKey);
		if (it != intern_roots.end()) {
			int32_t index = buffer.read_integral<int32_t>() ^1;
			return it->second->un_intern_value<T>(index);
		} else {
			return wrapper::make_wrapper<T>(readValueDelegate(*this, buffer));
		}
	}

	template<typename T, util::hash_t InternKey, typename F, typename>
	void SerializationCtx::writeInterned(Buffer const &buffer, const Wrapper<T> &value, F&& writeValueDelegate) const {
		auto it = intern_roots.find(InternKey);
		if (it != intern_roots.end()) {
			int32_t index = it->second->intern_value<T>(value);
			buffer.write_integral<int32_t>(index);
		} else {
			writeValueDelegate(*this, buffer, *value);
		}
	}
}
#endif //RD_CPP_FRAMEWORK_SERIALIZATIONCTX_H

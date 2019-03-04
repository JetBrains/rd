//
// Created by jetbrains on 20.07.2018.
//

#ifndef RD_CPP_FRAMEWORK_SERIALIZATIONCTX_H
#define RD_CPP_FRAMEWORK_SERIALIZATIONCTX_H

#include "InternRoot.h"
#include "Buffer.h"

#include <unordered_map>
#include <functional>
#include <string>
#include <utility>

namespace rd {
	//region predeclared

	class IProtocol;

	class Serializers;
	//endregion

	class SerializationCtx {
	public:
		using roots_t = std::unordered_map<RdId::hash_t, InternRoot const *>;

		Serializers const *serializers = nullptr;

		roots_t intern_roots{};

		//region ctor/dtor

		//    SerializationCtx() = delete;

		SerializationCtx(SerializationCtx &&other) noexcept = default;

		SerializationCtx &operator=(SerializationCtx &&other) noexcept = default;

//		explicit SerializationCtx(const Serializers *serializers = nullptr);

		explicit SerializationCtx(const Serializers *serializers, roots_t intern_roots = {});

		SerializationCtx withInternRootsHere(RdBindableBase const &owner, std::initializer_list<std::string> new_roots);

		//endregion

		template<typename T, RdId::hash_t InternKey>
		T readInterned(Buffer const &buffer, std::function<T(SerializationCtx const &, Buffer const &)> readValueDelegate) const {
			auto it = intern_roots.find(InternKey);
			if (it != intern_roots.end()) {
				return it->second->un_intern_value<T>(buffer.read_integral<int32_t>() ^ 1);
			} else {
				return readValueDelegate(*this, buffer);
			}
		}

		template<typename T, RdId::hash_t InternKey>
		void writeInterned(Buffer const &buffer, T const &value,
						   std::function<void(SerializationCtx const &, Buffer const &, T const &)> writeValueDelegate) const {
			auto it = intern_roots.find(InternKey);
			if (it != intern_roots.end()) {
				buffer.write_integral<int32_t>(it->second->intern_value<T>(value));
			} else {
				writeValueDelegate(*this, buffer, value);
			}
		}
	};
}

#endif //RD_CPP_FRAMEWORK_SERIALIZATIONCTX_H

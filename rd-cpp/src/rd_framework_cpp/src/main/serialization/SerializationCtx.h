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

namespace rd {
	//region predeclared

	class IProtocol;

	class Serializers;
	//endregion

	class SerializationCtx {
	public:
		Serializers const *serializers = nullptr;

//		template<hash_t InternKey>
		std::unordered_map<RdId::hash_t, std::shared_ptr<InternRoot>> intern_roots{};

		//    SerializationCtx() = delete;

		//region ctor/dtor


		SerializationCtx(SerializationCtx &&other) noexcept = default;

		SerializationCtx &operator=(SerializationCtx &&other) noexcept = default;

		explicit SerializationCtx(const Serializers *serializers = nullptr);

//		SerializationCtx(const Serializers *serializers, InternRoot internRoot);

		template<RdId::hash_t... R>
		SerializationCtx withInternRootsHere(RdBindableBase const &owner) {
			auto next_roots = intern_roots;
			return SerializationCtx(nullptr);
		}

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

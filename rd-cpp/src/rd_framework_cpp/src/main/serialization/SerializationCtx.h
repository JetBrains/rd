//
// Created by jetbrains on 20.07.2018.
//

#ifndef RD_CPP_FRAMEWORK_SERIALIZATIONCTX_H
#define RD_CPP_FRAMEWORK_SERIALIZATIONCTX_H

#include "Buffer.h"
#include "InternRoot.h"

namespace rd {
    class IProtocol;
    class Serializers;

	class SerializationCtx {
	public:
		Serializers const *serializers = nullptr;

		tl::optional<InternRoot> internRoot;

		//    SerializationCtx() = delete;

		//region ctor/dtor

		SerializationCtx(SerializationCtx &&other) noexcept = default;

		SerializationCtx &operator=(SerializationCtx &&other) noexcept = default;

		explicit SerializationCtx(const Serializers *serializers = nullptr);

		SerializationCtx(const Serializers *serializers, InternRoot internRoot);

		explicit SerializationCtx(IProtocol const &protocol);
		//endregion

		SerializationCtx withInternRootHere(bool isMaster) const;

		template<typename T>
		T readInterned(Buffer const &buffer,
		               std::function<T(SerializationCtx const &, Buffer const &)> readValueDelegate) const {
			return T();
			//todo implement
		}

		template<typename T>
		void writeInterned(Buffer const &buffer, T const &value,
		                   std::function<void(SerializationCtx const &, Buffer const &,
		                                      T const &)> writeValueDelegate) const {
			//todo implement
		}
	};
}

#endif //RD_CPP_FRAMEWORK_SERIALIZATIONCTX_H

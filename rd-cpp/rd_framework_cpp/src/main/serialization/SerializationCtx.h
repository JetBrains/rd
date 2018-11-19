//
// Created by jetbrains on 20.07.2018.
//

#ifndef RD_CPP_FRAMEWORK_SERIALIZATIONCTX_H
#define RD_CPP_FRAMEWORK_SERIALIZATIONCTX_H

#include "Buffer.h"
#include "Serializers.h"


class IProtocol;

class SerializationCtx {
public:
    Serializers const *serializers = nullptr;
//    tl::optional<IInternRoot> internRoot;

//    SerializationCtx() = delete;

    //region ctor/dtor

    SerializationCtx(SerializationCtx &&other) noexcept = default;

    SerializationCtx &operator=(SerializationCtx &&other) noexcept = default;

    SerializationCtx(const Serializers *serializers = nullptr);

    explicit SerializationCtx(IProtocol const &protocol);
    //endregion
};

#endif //RD_CPP_FRAMEWORK_SERIALIZATIONCTX_H

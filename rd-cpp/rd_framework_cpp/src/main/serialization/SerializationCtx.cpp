//
// Created by jetbrains on 20.07.2018.
//

#include "IProtocol.h"

SerializationCtx::SerializationCtx(const IProtocol &protocol) : serializers(&protocol.serializers) {}

SerializationCtx::SerializationCtx(const Serializers *const serializers) : serializers(serializers) {}

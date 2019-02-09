//
// Created by jetbrains on 20.07.2018.
//

#include "SerializationCtx.h"

#include "IProtocol.h"

SerializationCtx::SerializationCtx(const IProtocol &protocol) : serializers(&protocol.serializers) {}

SerializationCtx::SerializationCtx(const Serializers *const serializers) : serializers(serializers) {}

SerializationCtx SerializationCtx::withInternRootHere(bool isMaster) const {
    return SerializationCtx(serializers, InternRoot(isMaster));
}

SerializationCtx::SerializationCtx(const Serializers *serializers, InternRoot internRoot) :
        serializers(serializers), internRoot(std::move(internRoot)) {

}

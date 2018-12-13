//
// Created by jetbrains on 30.07.2018.
//

#ifndef RD_CPP_ISERIALIZER_H
#define RD_CPP_ISERIALIZER_H

#include "SerializationCtx.h"
#include "Buffer.h"
#include "ISerializable.h"

template<typename T>
class ISerializer {
public:
    virtual ISerializable const &read(SerializationCtx const &ctx, Buffer const &buffer) = 0;

    virtual void write(SerializationCtx const &ctx, Buffer const &buffer, T const &value) = 0;
};


#endif //RD_CPP_ISERIALIZER_H

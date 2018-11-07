//
// Created by jetbrains on 07.08.2018.
//

#ifndef RD_CPP_ISERIALIZABLE_H
#define RD_CPP_ISERIALIZABLE_H


#include "RdId.h"

//#include "SerializationCtx.h"
class SerializationCtx;

//template <int id>
class ISerializable {
public:
    virtual ~ISerializable() = default;

    virtual void write(SerializationCtx const &ctx, Buffer const &buffer) const = 0;
};


#endif //RD_CPP_ISERIALIZABLE_H

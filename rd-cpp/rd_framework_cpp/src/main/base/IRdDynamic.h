//
// Created by jetbrains on 30.07.2018.
//

#ifndef RD_CPP_IRDDYNAMIC_H
#define RD_CPP_IRDDYNAMIC_H

#include "RName.h"
#include "SerializationCtx.h"

//class IProtocol;

class IRdDynamic {
public:
    SerializationCtx serialization_context;
    mutable RName location;

    //region ctor/dtor

    IRdDynamic() = default;

    IRdDynamic(IRdDynamic &&other) = default;

    IRdDynamic &operator=(IRdDynamic &&other) = default;

    virtual ~IRdDynamic() = default;
    //endregion

    virtual const IProtocol *const get_protocol() const = 0;

    virtual SerializationCtx const &get_serialization_context() const = 0;
};


#endif //RD_CPP_IRDDYNAMIC_H

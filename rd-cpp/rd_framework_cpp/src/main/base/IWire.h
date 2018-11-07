//
// Created by jetbrains on 30.07.2018.
//

#ifndef RD_CPP_IWIRE_H
#define RD_CPP_IWIRE_H


#include "interfaces.h"
#include "IRdReactive.h"
#include "Property.h"

class IWire {
public:
    Property<bool> connected{false};

    //region ctor/dtor

    IWire() = default;

    IWire(IWire &&) = default;

    virtual ~IWire() = default;
    //endregion

    virtual void advise(Lifetime lifetime, IRdReactive const *entity) const = 0;

    virtual void send(RdId const &id, std::function<void(Buffer const &buffer)> writer) const = 0;
};


#endif //RD_CPP_IWIRE_H

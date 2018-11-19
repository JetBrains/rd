//
// Created by jetbrains on 25.07.2018.
//

#ifndef RD_CPP_PROTOCOL_H
#define RD_CPP_PROTOCOL_H


#include "IProtocol.h"

class Protocol : /*IRdDynamic, */public IProtocol {
public:
    //region ctor/dtor
    Protocol(std::shared_ptr<IIdentities> identity, const IScheduler *const scheduler, std::shared_ptr<IWire> wire);

    Protocol(Identities &&identity, const IScheduler *const scheduler, std::shared_ptr<IWire> wire);

    Protocol(Protocol const &) = delete;
    //endregion

    IProtocol const *const get_protocol() const override;

    inline static const Logger initializationLogger;
};


#endif //RD_CPP_PROTOCOL_H

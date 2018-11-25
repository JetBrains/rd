//
// Created by jetbrains on 25.07.2018.
//

#ifndef RD_CPP_PROTOCOL_H
#define RD_CPP_PROTOCOL_H


#include "IProtocol.h"

class Protocol : /*IRdDynamic, */public IProtocol {
public:
    //region ctor/dtor
    Protocol(std::shared_ptr<IIdentities> identity, IScheduler *scheduler, std::shared_ptr<IWire> wire);

    Protocol(Identities &&identity, IScheduler *scheduler, std::shared_ptr<IWire> wire);

    Protocol(Protocol const &) {
        assert("What the actual fuck" && false);
    };

    Protocol(Protocol &&) = default;

    Protocol &operator=(Protocol &&) = default;
    //endregion

    IProtocol const *const get_protocol() const override;

    static const Logger initializationLogger;
};


#endif //RD_CPP_PROTOCOL_H

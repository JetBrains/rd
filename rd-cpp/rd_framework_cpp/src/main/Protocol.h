//
// Created by jetbrains on 25.07.2018.
//

#ifndef RD_CPP_PROTOCOL_H
#define RD_CPP_PROTOCOL_H


#include "IProtocol.h"
#include "interfaces.h"


class Protocol : /*IRdDynamic, */public IProtocol {
public:
    //region ctor/dtor
    Protocol(std::shared_ptr<IIdentities> identity, const IScheduler *const scheduler, std::shared_ptr<IWire> wire) :
            IProtocol(std::move(identity), scheduler, std::move(wire)) {}

    Protocol(Identities &&identity, const IScheduler *const scheduler, std::shared_ptr<IWire> wire) :
            IProtocol(std::make_shared<Identities>(identity), scheduler, std::move(wire)) {}

	Protocol(Protocol const &) {
		assert("What the actual fuck" && false);
	};
	Protocol(Protocol &&) = default;
	Protocol& operator = (Protocol&&) = default;
    //endregion

    IProtocol const *const get_protocol() const override;

    static const Logger initializationLogger;
};


#endif //RD_CPP_PROTOCOL_H

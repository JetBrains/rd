//
// Created by jetbrains on 30.07.2018.
//

#ifndef RD_CPP_IPROTOCOL_H
#define RD_CPP_IPROTOCOL_H


#include "IScheduler.h"
#include "IIdentities.h"
#include "IWire.h"
#include "Serializers.h"

class IRdDynamic;

class IProtocol : public IRdDynamic {
public:
	IProtocol() = default;
    Serializers serializers;
    std::shared_ptr<IIdentities> identity;
    IScheduler const *scheduler;
    std::shared_ptr<IWire> wire;
    SerializationCtx context;

    //region ctor/dtor

    IProtocol(std::shared_ptr<IIdentities> identity, const IScheduler *scheduler, std::shared_ptr<IWire> wire);

    virtual ~IProtocol() = default;
    //endregion

    const SerializationCtx &get_serialization_context() const override;
};


#endif //RD_CPP_IPROTOCOL_H

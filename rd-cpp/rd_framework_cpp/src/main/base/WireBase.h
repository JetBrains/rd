//
// Created by jetbrains on 25.07.2018.
//

#ifndef RD_CPP_WIREBASE_H
#define RD_CPP_WIREBASE_H


#include "Property.h"
#include "IWire.h"
#include "MessageBroker.h"

class WireBase : public IWire {
protected:
    IScheduler const *const scheduler = nullptr;

    MessageBroker message_broker;
public:
    //region ctor/dtor

    WireBase(WireBase &&) = default;

    explicit WireBase(IScheduler const *const scheduler) : scheduler(scheduler), message_broker(scheduler) {}

    virtual ~WireBase() = default;
    //endregion

    void advise(Lifetime lifetime, IRdReactive const *entity) const override;

    void dumpToString() {
//        messageBroker.printToString()''
    }
};


#endif //RD_CPP_WIREBASE_H

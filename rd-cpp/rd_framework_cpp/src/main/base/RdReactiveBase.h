//
// Created by jetbrains on 23.07.2018.
//

#ifndef RD_CPP_RDREACTIVEBASE_H
#define RD_CPP_RDREACTIVEBASE_H


#include "RdBindableBase.h"
#include "IRdReactive.h"
#include "IWire.h"
#include "IProtocol.h"
#include "Logger.h"

class RdReactiveBase : public RdBindableBase, public IRdReactive {
public:
    Logger logReceived;
    Logger logSend;

    //region ctor/dtor

    RdReactiveBase() = default;

    RdReactiveBase(RdReactiveBase &&other);

    RdReactiveBase &operator=(RdReactiveBase &&other);

    virtual ~RdReactiveBase() = default;
    //endregion

    const IWire *const get_wire() const;

    mutable bool is_local_change = false;

    //delegated
    const Serializers &get_serializers() {
        return get_protocol()->serializers;
    }

    const Serializers &get_serializers() const;

    const IScheduler *get_default_scheduler() const;

    const IScheduler *const get_wire_scheduler() const;

    void assert_threading() const;

    void assert_bound() const;

    template<typename T, typename F>
    T local_change(F &&action) const {
        if (is_bound()) {
            assert_threading();
        }

        MY_ASSERT_MSG(!is_local_change, "!isLocalChange for RdReactiveBase with id:" + rdid.toString());

        is_local_change = true;
        T res = action();
        is_local_change = false;
        return res;
    }

    void local_change(const std::function<void()> &action) const;
    //todo catch exception in action()
};


#endif //RD_CPP_RDREACTIVEBASE_H

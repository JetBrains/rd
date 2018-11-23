//
// Created by jetbrains on 01.11.2018.
//

#ifndef RD_CPP_RDENDPOINT_H
#define RD_CPP_RDENDPOINT_H

#include "Polymorphic.h"
#include "RdTask.h"

template<typename TReq, typename TRes, typename ReqSer = Polymorphic<TReq>, typename ResSer = Polymorphic<TRes> >
class RdEndpoint : public RdReactiveBase, public ISerializable {
    using handler_t = std::function<RdTask<TRes>(Lifetime, TReq const &)>;
    handler_t handler;

public:
    //region ctor/dtor

    explicit RdEndpoint(handler_t handler) : handler(std::move(handler)) {}

    explicit RdEndpoint(std::function<TRes(TReq const &)> handler) : handler([handler = std::move(handler)](Lifetime _, TReq const &req) {
        return RdTask<TRes, ResSer>::from_result(std::move(handler(req)));
    }) {}
    //endregion

    static RdEndpoint<TReq, TRes, ReqSer, ResSer> read(SerializationCtx const &ctx, Buffer const &buffer) {
        RdEndpoint<TReq, TRes, ReqSer, ResSer> res;
        const RdId &id = RdId::read(buffer);
        withId(res, id);
        return res;
    }

    void write(SerializationCtx const &ctx, Buffer const &buffer) const override {
        rdid.write(buffer);
    }

    void set(handler_t handler) {
        MY_ASSERT_MSG(handler, "handler is set already");
        this->handler = std::move(handler);

    }

    void init(Lifetime lifetime) const override {
        RdReactiveBase::init(lifetime);
        this->bind_lifetime = lifetime;

        get_wire()->advise(lifetime, this);
    }

    void on_wire_received(Buffer buffer) const override {
        auto taskId = RdId::read(buffer);
        auto value = ReqSer::read(get_serialization_context(), buffer);
        logReceived.trace(
                "endpoint " + location.toString() + " ::" + rdid.toString() + " request = ${value.printToString()}");
        //todo little bit monadic programming here
        if (!handler) {
            throw std::invalid_argument("handler is empty for RdEndPoint");
        }
        RdTask<TRes, ResSer> task;
        try {
            task = handler(*bind_lifetime, value);
        } catch (std::exception const &e) {
            task.fault(e);
        }
        task.advise(*bind_lifetime, [&](RdTaskResult<TRes, ResSer> const &taskResult) {
            logSend.trace("endpoint " + location.toString() + " ::(" + rdid.toString() +
                          ") response = ${result.printToString()}");
            get_wire()->send(rdid, [&](Buffer const &buffer) {
                taskId.write(buffer);
                taskResult.write(get_serialization_context(), buffer);
            });
        });
    }
};


#endif //RD_CPP_RDENDPOINT_H

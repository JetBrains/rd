//
// Created by jetbrains on 01.11.2018.
//

#ifndef RD_CPP_RDCALL_H
#define RD_CPP_RDCALL_H

#include <thread>

#include "Polymorphic.h"
#include "RdTask.h"
#include "RdTaskResult.h"

template<typename TReq, typename TRes, typename ReqSer = Polymorphic<TReq>, typename ResSer = Polymorphic<TRes> >
class RdCall : public RdReactiveBase, public ISerializable {
    mutable std::unordered_map<RdId, std::pair<IScheduler const *, RdTask<TRes, ResSer>>> requests;
    std::optional<RdId> syncTaskId;

public:

    static RdCall<TReq, TRes, ReqSer, ResSer> read(SerializationCtx const &ctx, Buffer const &buffer) {
        RdCall<TReq, TRes, ReqSer, ResSer> res;
        const RdId &id = RdId::read(buffer);
        withId(res, id);
        return res;
    }

    void write(SerializationCtx const &ctx, Buffer const &buffer) const override {
        rdid.write(buffer);
    }

    void init(Lifetime lifetime) const override {
        RdBindableBase::init(lifetime);

        get_wire()->advise(lifetime, this);

        lifetime->add_action([this]() {
            for (auto const &req : requests) {
                RdTask<TRes, ResSer> task = req.second.second;
                if (!task.has_value()) {//todo race
                    task.cancel();
                }
            }

            requests.clear();
        });
    }

    void on_wire_received(Buffer buffer) const override {
        RdId taskId = RdId::read(buffer);

        if (requests.count(taskId) == 0) {
            logReceived.trace("call " + location.toString() + " " + rdid.toString() +
                              "received response " + taskId.toString() + " but it was dropped");
        } else {
            auto const &request = requests.at(taskId);
            auto result = RdTaskResult<TRes, ResSer>::read(get_serialization_context(), buffer);
            logReceived.trace("call " + location.toString() + " " + rdid.toString() +
                              " received response " + taskId.toString() + " : ${result.printToString()} ");

            auto const&[scheduler, task] = request;
            /*auto scheduler = request.first;
            auto const &task = request.second;*/
            scheduler->queue([&]() {
                if (task.has_value()) {
                    logReceived.trace("call " + location.toString() + " " + rdid.toString() +
                                      " response was dropped, task result is: ${task.result.valueOrNull}");
                    if (is_bound() && get_default_scheduler()->is_active() && requests.count(taskId) > 0) {
//                        logAssert.error("MainThread: ${defaultScheduler.isActive}, taskId=$taskId ");
                    }
                } else {
                    //todo could be race condition in sync mode in case of Timeout, but it's not really valid case
                    //todo but now we could start task on any scheduler - need interlocks in property
                    task.set_result(std::move(result));
                    requests.erase(taskId);
                }
            });
        }
    }

    TRes sync(TReq const &request) {
        try {
            RdTask task = startInternal(request, true, get_default_scheduler());//todo SynchronousScheduler
            while (!task.has_value()) {
                std::this_thread::yield();
            }
            auto res = task.value_or_throw().unwrap();
            syncTaskId = std::nullopt;
            return res;
        } catch (...) {
            throw;
        }
    }

    RdTask<TRes, ResSer> start(TReq const &request, IScheduler const *responseScheduler) {
        return startInternal(request, false, responseScheduler ? responseScheduler : get_default_scheduler());
    }

    RdTask<TRes, ResSer> start(TReq const &request) {
        return start(request, nullptr);
    }


    RdTask<TRes, ResSer> startInternal(TReq const &request, bool sync, IScheduler const *scheduler) {
        assert_bound();
        if (!async) {
            assert_threading();
        }

        RdId taskId = get_protocol()->identity->next(rdid);
        MY_ASSERT_MSG(requests.count(taskId) == 0, "requests already contain task with id:" + taskId.toString());
        RdTask<TRes, ResSer> task;
        auto pair = std::make_pair(scheduler, task);
        requests.emplace(taskId, std::move(pair));

        if (sync) {
            if (syncTaskId.has_value()) {
                throw std::invalid_argument(
                        "Already exists sync task for call " + location.toString() + ", taskId = " +
                        (*syncTaskId).toString());
            }
            syncTaskId = taskId;
        }

        get_wire()->send(rdid, [&](Buffer const &buffer) {
            logSend.trace("call " + location.toString() + "::" + rdid.toString() + " send" + (sync ? "SYNC" : "ASYNC") +
                          " request " + taskId.toString() + " : ${request.printToString()} ");
            taskId.write(buffer);
            ReqSer::write(get_serialization_context(), buffer, request);
        });

        return task;
    }

};


#endif //RD_CPP_RDCALL_H

//
// Created by jetbrains on 01.11.2018.
//

#ifndef RD_CPP_RDTASK_H
#define RD_CPP_RDTASK_H

#include "RdTaskResult.h"
#include "RdTaskImpl.h"
#include "Polymorphic.h"

#include <functional>


template<typename T, typename S = Polymorphic<T> >
class RdTask {
private:
    mutable std::shared_ptr<RdTaskImpl<T, S> > ptr{std::make_shared<RdTaskImpl<T, S> >()};
public:

    using result_type = RdTaskResult<T, S>;

    static RdTask<T, S> from_result(T value) {
        RdTask<T, S> res;
        res.set(std::move(value));
        return res;
    }

    void set(T value) const {
        typename RdTaskResult<T, S>::Success t(std::move(value));
        ptr->result.set(tl::make_optional(std::move(t)));
    }

    void set_result(RdTaskResult<T, S> value) const {
        ptr->result.set(tl::make_optional(std::move(value)));
    }

    void cancel() const {
        ptr->result.set(typename RdTaskResult<T, S>::Cancelled());
    }

    void fault(std::exception const &e) const {
        ptr->result.set(typename RdTaskResult<T, S>::Fault(e));
    }

    bool has_value() const {
        return ptr->result.get().has_value();
    }

    RdTaskResult<T, S> value_or_throw() const {
        auto const &opt_res = ptr->result.get();
        if (opt_res.has_value()) {
            return *opt_res;
        }
        throw std::runtime_error("task is empty");
    }

    bool isFaulted() const {
        return has_value() && value_or_throw().isFaulted(); //todo atomicy
    }

    void advise(Lifetime lifetime, std::function<void(RdTaskResult<T, S> const &)> handler) const {
        ptr->result.advise(lifetime, [handler = std::move(handler)](tl::optional<RdTaskResult<T, S> > const &opt_value) {
            if (opt_value.has_value()) {
                handler(*opt_value);
            }
        });
    }
};


#endif //RD_CPP_RDTASK_H

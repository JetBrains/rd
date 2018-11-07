//
// Created by jetbrains on 01.11.2018.
//

#ifndef RD_CPP_RDTASK_H
#define RD_CPP_RDTASK_H

#include <functional>

#include "Property.h"
#include "RdTaskResult.h"
#include "RdTaskImpl.h"
#include "Polymorphic.h"

template<typename T, typename S = Polymorphic<T> >
class RdTask {

    std::shared_ptr<RdTaskImpl<T, S> > ptr = std::make_shared<RdTaskImpl<T, S> >();
public:

    static RdTask<T, S> from_result(T value) {
        RdTask<T, S> res;
        res.set(std::move(value));
        return res;
    }

    void set(T value) const {
        auto t = typename RdTaskResult<T, S>::Success(std::move(value));
        ptr->result.set(std::make_optional(std::move(t)));
    }

    void set_result(RdTaskResult<T, S> value) const {
        ptr->result.set(std::make_optional(std::move(value)));
    }

    void cancel() const {
        ptr->result.set(typename RdTaskResult<T>::Cancelled());
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

    void advise(Lifetime lifetime, std::function<void(RdTaskResult<T, S> const &)> handler) const {
        /*ptr->result.advise(lifetime, [](std::optional<RdTaskResult<T, S> > const &opt_value) {
//            if (opt_value)
        });*/
    }
};


#endif //RD_CPP_RDTASK_H

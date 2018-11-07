//
// Created by jetbrains on 17.08.2018.
//

#ifndef RD_CPP_IPROPERTY_H
#define RD_CPP_IPROPERTY_H

#include <memory>
#include <functional>

#include "SignalX.h"
#include "Lifetime.h"
#include "IPropertyBase.h"

template<typename T>
class IProperty : public IPropertyBase<T> {
public:

    //region ctor/dtor

    IProperty(IProperty &&other) = default;

    IProperty &operator=(IProperty &&other) = default;

    explicit IProperty(T const &value) : IPropertyBase<T>(value) {}

    explicit IProperty(T &&value) : IPropertyBase<T>(std::move(value)) {}

    virtual ~IProperty() = default;
    //endregion

    virtual T const &get() const = 0;

    void advise0(Lifetime lifetime, std::function<void(T const &)> handler, Signal<T> const &signal) const {
        if (lifetime->is_terminated()) {
            return;
        }
        signal.advise(std::move(lifetime), handler);
        handler(this->value);
    }

    void advise_before(Lifetime lifetime, std::function<void(T const &)> handler) const override {
        advise0(lifetime, handler, this->before_change);
    }

    void advise(Lifetime lifetime, std::function<void(T const &)> handler) const override {
        advise0(std::move(lifetime), std::move(handler), this->change);
    }

    virtual void set(T) const = 0;
};


#endif //RD_CPP_IPROPERTY_H

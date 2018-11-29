//
// Created by jetbrains on 20.08.2018.
//

#ifndef RD_CPP_IPROPERTYBASE_H
#define RD_CPP_IPROPERTYBASE_H


#include <interfaces.h>
#include <SignalX.h>

#include "optional.hpp"

template<typename T>
class IPropertyBase : public ISource<T>, public IViewable<T> {
protected:
    mutable tl::optional<T> value{};

    Signal<T> change, before_change;

	bool has_value() const {
		return value.has_value();
	}

public:

    //region ctor/dtor

	IPropertyBase() = default;

    IPropertyBase(IPropertyBase &&other) = default;

    IPropertyBase &operator=(IPropertyBase &&other) = default;

    explicit IPropertyBase(T const &value) : value(value) {}

    explicit IPropertyBase(T &&value) : value(std::move(value)) {}

    virtual ~IPropertyBase() = default;
    //endregion

    virtual void advise_before(Lifetime lifetime, std::function<void(T const &)> handler) const = 0;

    void view(Lifetime lifetime, std::function<void(Lifetime, T const &)> handler) const override {
        if (lifetime->is_terminated()) return;

        Lifetime lf = lifetime.create_nested();
        std::shared_ptr<SequentialLifetimes> seq = std::make_shared<SequentialLifetimes>(lf);

        this->advise_before(lf, [this, lf, seq](T const &v) {
            if (!lf->is_terminated()) {
                seq->terminate_current();
            }
        });

        this->advise(lf, [this, lf, seq, handler](T const &v) {
            if (!lf->is_terminated()) {
                handler(seq->next(), v);
            }
        });
    }
};


#endif //RD_CPP_IPROPERTYBASE_H

//
// Created by jetbrains on 25.07.2018.
//

#ifndef RD_CPP_RDPROPERTYBASE_H
#define RD_CPP_RDPROPERTYBASE_H


#include "RdReactiveBase.h"
#include "ISerializer.h"
#include "SignalX.h"
#include "Polymorphic.h"
#include "Property.h"

template<typename T, typename S = Polymorphic<T>>
class RdPropertyBase : public RdReactiveBase, public Property<T> {
protected:
    //mastering
    bool is_master = true;
    mutable int32_t master_version = 0;
    mutable bool default_value_changed = false;

    //init
public:
    bool optimizeNested = false;

    //region ctor/dtor

    RdPropertyBase(RdPropertyBase const &) = delete;

    RdPropertyBase(RdPropertyBase &&other) = default;

    RdPropertyBase &operator=(RdPropertyBase &&other) = default;

    explicit RdPropertyBase(const T &value) : Property<T>(value) {}

    explicit RdPropertyBase(T &&value) : Property<T>(std::move(value)) {}

    virtual ~RdPropertyBase() = default;
    //endregion

    void init(Lifetime lifetime) const override {
        RdReactiveBase::init(lifetime);


        if (!optimizeNested) {
            this->change.advise(lifetime, [this](T const &v) {
                if (is_local_change) {
                    const IProtocol *iProtocol = get_protocol();
                    identifyPolymorphic(v, *iProtocol->identity, iProtocol->identity->next(rdid));
                }
            });
        }

        advise(lifetime, [this](T const &v) {
            if (!is_local_change) {
                return;
            }
            if (is_master) {
                master_version++;
            }
            get_wire()->send(rdid, [this, &v](Buffer const &buffer) {
                buffer.write_pod<int32_t>(master_version);
                S::write(this->get_serialization_context(), buffer, v);
                this->logSend.trace("property " + location.toString() + " + " + rdid.toString() +
                                    ":: ver = " + std::to_string(master_version) +
                                    ", value = " + to_string(v));
            });
        });

        get_wire()->advise(lifetime, this);

        if (!optimizeNested) {
            this->view(lifetime, [this](Lifetime lf, T const &v) {
                bindPolymorphic(v, lf, this, "$");
            });
        }
    }

    void on_wire_received(Buffer buffer) const override {
        int32_t version = buffer.read_pod<int32_t>();
        T v = S::read(this->get_serialization_context(), buffer);

        bool rejected = is_master && version < master_version;
        if (rejected) {
            return;
        }
        master_version = version;

        Property<T>::set(std::move(v));
    };

    void advise(Lifetime lifetime, std::function<void(const T &)> handler) const override {
        if (is_bound()) {
//            assertThreading();
        }
        Property<T>::advise(lifetime, handler);
    }


    T const &get() const override {
        return this->value;
    }

    void set(T new_value) const override {
        this->local_change([this, &new_value]() mutable {
            this->default_value_changed = true;
            Property<T>::set(std::move(new_value));
        });
    }

    friend bool operator==(const RdPropertyBase &lhs, const RdPropertyBase &rhs) {
        return &lhs == &rhs;
    }

    friend bool operator!=(const RdPropertyBase &lhs, const RdPropertyBase &rhs) {
        return !(rhs == lhs);
    }
};

static_assert(std::is_move_constructible_v<RdPropertyBase<int> >);

#endif //RD_CPP_RDPROPERTYBASE_H

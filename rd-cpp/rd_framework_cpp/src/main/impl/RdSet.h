//
// Created by jetbrains on 02.08.2018.
//

#ifndef RD_CPP_RDSET_H
#define RD_CPP_RDSET_H


#include "ViewableSet.h"
#include "RdReactiveBase.h"
#include "Polymorphic.h"

template<typename T, typename S = Polymorphic<T>>
class RdSet : public RdReactiveBase, public IViewableSet<T> {
protected:
    ViewableSet<T> set;

    using Event = typename IViewableSet<T>::Event;

public:
    //region ctor/dtor

    RdSet() = default;

    RdSet(RdSet &&) = default;

    RdSet &operator=(RdSet &&) = default;

    virtual ~RdSet() = default;

    //endregion

    bool optimizeNested = false;

    void init(Lifetime lifetime) const override {
        RdBindableBase::init(lifetime);

        local_change([this, lifetime]() {
            advise(lifetime, [this](AddRemove kind, T const &v) {
                if (!is_local_change) return;

                get_wire()->send(rdid, [this, kind, v](Buffer const &buffer) {
                    buffer.writeEnum<AddRemove>(kind);
                    S::write(this->get_serialization_context(), buffer, v);

                    this->logSend.trace(
                            "set " + location.toString() + " " + rdid.toString() +
                            ":: " + to_string(kind) +
                            ":: " + to_string(v));
                });
            });
        });

        get_wire()->advise(lifetime, this);
    }

    void on_wire_received(Buffer buffer) const override {
        AddRemove kind = buffer.readEnum<AddRemove>();
        T value = S::read(this->get_serialization_context(), buffer);

        switch (kind) {
            case AddRemove::ADD : {
                set.add(value);
                break;
            }
            case AddRemove::REMOVE: {
                set.remove(value);
                break;
            }
        }
    }

    bool add(T value) const override {
        return local_change<bool>([&]() { return set.add(std::move(value)); });
    }

    void clear() const override {
        return local_change([&]() { return set.clear(); });
    }

    bool remove(T const &value) const override {
        return local_change<bool>([&]() { return set.remove(value); });
    }

    size_t size() const override {
        return local_change<size_t>([&]() { return set.size(); });
    }

    bool contains(T const &value) const override {
        return local_change<bool>([&]() { return set.contains(value); });
    }

    bool empty() const override {
        return local_change<bool>([&]() { return set.empty(); });
    }

    void advise(Lifetime lifetime, std::function<void(Event)> handler) const override {
        if (is_bound()) {
            assert_threading();
        }
        set.advise(lifetime, handler);
    }

    using IViewableSet<T>::advise;
};

static_assert(std::is_move_constructible_v<RdSet<int> >);

#endif //RD_CPP_RDSET_H

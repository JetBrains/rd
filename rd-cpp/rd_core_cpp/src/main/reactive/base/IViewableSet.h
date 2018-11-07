//
// Created by jetbrains on 14.08.2018.
//

#ifndef RD_CPP_IVIEWABLESET_H
#define RD_CPP_IVIEWABLESET_H


#include "interfaces.h"
#include "core_util.h"
#include "viewable_collections.h"

template<typename T>
class IViewableSet : public IViewable<T> {
protected:
    mutable std::unordered_map<Lifetime, std::unordered_map<T, LifetimeDefinition>, Lifetime::Hash> lifetimes;
public:

    class Event {
    public:
        Event(AddRemove kind, T const *value) : kind(kind), value(value) {}

        AddRemove kind;
        T const *value;
    };

    virtual ~IViewableSet() = default;

    virtual void advise(Lifetime lifetime, std::function<void(AddRemove, T const &)> handler) const {
        this->advise(lifetime, [handler](Event e) {
            handler(e.kind, *e.value);
        });
    }


    void view(Lifetime lifetime, std::function<void(Lifetime, T const &)> handler) const override {
        advise(lifetime, [this, lifetime, handler](AddRemove kind, T const &key) {
            switch (kind) {
                case AddRemove::ADD: {
                    auto const &[it, inserted] = lifetimes[lifetime].emplace(key, LifetimeDefinition(lifetime));
                    MY_ASSERT_MSG(inserted, "lifetime definition already exists in viewable set by key:" + to_string(key));
                    handler(it->second.lifetime, key);
                    break;
                }
                case AddRemove::REMOVE: {
                    MY_ASSERT_MSG(lifetimes.at(lifetime).count(key) > 0, "attempting to remove non-existing lifetime in viewable set by key:" + to_string(key));
                    LifetimeDefinition def = std::move(lifetimes.at(lifetime).at(key));
                    lifetimes.at(lifetime).erase(key);
                    def.terminate();
                    break;
                }
            }
        });
    }

    virtual void advise(Lifetime lifetime, std::function<void(Event)> handler) const = 0;

    virtual bool add(T) const = 0;

    virtual void clear() const = 0;

    virtual bool remove(T const &) const = 0;

    virtual size_t size() const = 0;

    virtual bool contains(T const &) const = 0;

    virtual bool empty() const = 0;
};

static_assert(std::is_move_constructible_v<IViewableSet<int>::Event>);

#endif //RD_CPP_IVIEWABLESET_H

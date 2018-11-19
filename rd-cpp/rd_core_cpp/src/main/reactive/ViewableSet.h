//
// Created by jetbrains on 10.07.2018.
//

#ifndef RD_CPP_CORE_VIEWABLESET_H
#define RD_CPP_CORE_VIEWABLESET_H

#include <ordered-map/include/tsl/ordered_set.h>

#include "base/IViewableSet.h"
#include "SignalX.h"
#include "util/core_util.h"


template<typename T>
class ViewableSet : public IViewableSet<T> {
public:
    using Event = typename IViewableSet<T>::Event;

    using IViewableSet<T>::advise;
private:
    Signal<Event> change;

    mutable tsl::ordered_set<std::unique_ptr<T>, HashSmartPtr<T>, KeyEqualSmartPtr<T>> set;

public:
    //region ctor/dtor


    ViewableSet() = default;

    ViewableSet(ViewableSet &&) = default;

    ViewableSet &operator=(ViewableSet &&) = default;

    virtual ~ViewableSet() = default;

    //endregion

    bool add(T element) const override {
        /*auto const &[it, success] = set.emplace(std::make_unique<T>(std::move(element)));*/
		auto const &it = set.emplace(std::make_unique<T>(std::move(element)));
        if (!it.second) {
            return false;
        }
        change.fire(Event(AddRemove::ADD, it.first->get()));
        return true;
    }

    //addAll(collection)?

    void clear() const override {
        std::vector<Event> changes;
        for (auto const &element : set) {
            changes.push_back(Event(AddRemove::REMOVE, element.get()));
        }
        for (auto const &e : changes) {
            change.fire(e);
        }
        set.clear();
    }

    bool remove(T const &element) const override {
        if (!contains(element)) {
            return false;
        }
        auto it = set.find(element);
        change.fire(Event(AddRemove::REMOVE, it->get()));
        set.erase(it);
        return true;
    }

    void advise(Lifetime lifetime, std::function<void(Event)> handler) const override {
        for (auto const &x : set) {
            handler(Event(AddRemove::ADD, x.get()));
        }
        change.advise(lifetime, handler);
    }

    size_t size() const override {
        return set.size();
    }

    bool contains(T const &element) const override {
        return set.count(element) > 0;
    }

    bool empty() const override {
        return set.empty();
    }
};

static_assert(std::is_move_constructible_v<ViewableSet<int> >, "Is move constructible from ViewableSet<int>");

#endif //RD_CPP_CORE_VIEWABLESET_H

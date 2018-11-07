//
// Created by jetbrains on 10.07.2018.
//

#ifndef RD_CPP_CORE_VIEWABLE_MAP_H
#define RD_CPP_CORE_VIEWABLE_MAP_H

#include <ordered-map/include/tsl/ordered_map.h>

#include "Logger.h"
#include "base/IViewableMap.h"
#include "SignalX.h"
#include "util/core_util.h"

template<typename K, typename V>
class ViewableMap : public IViewableMap<K, V> {
public:
    using Event = typename IViewableMap<K, V>::Event;
private:
    mutable tsl::ordered_map<std::shared_ptr<K>, std::shared_ptr<V>, HashSharedPtr<K>, KeyEqualSharedPtr<K>> map;
    Signal<Event> change;

public:
    //region ctor/dtor

    ViewableMap() = default;

    ViewableMap(ViewableMap &&) = default;

    ViewableMap &operator=(ViewableMap &&) = default;

    virtual ~ViewableMap() = default;
    //endregion

    void advise(Lifetime lifetime, std::function<void(Event)> handler) const override {
        change.advise(std::move(lifetime), handler);
        for (auto const &[key, value] : map) {
            handler(Event(typename Event::Add(key.get(), value.get())));;
        }
    }

    const V * get(K const &key) const override {
        auto it = map.find(key);
        if (it == map.end()) {
            return nullptr;
        }
        return it->second.get();
    }

    const V *set(K key, V value) const override {
        if (map.count(key) == 0) {
            auto[it, success] = map.emplace(std::make_shared<K>(std::move(key)), std::make_shared<V>(std::move(value)));
            auto const &key_ptr = it->first;
            auto const &value_ptr = it->second;
            change.fire(typename Event::Add(key_ptr.get(), value_ptr.get()));
            return nullptr;
        } else {
            auto it = map.find(key);
            auto const &key_ptr = it->first;
            auto const &value_ptr = it->second;

            if (*value_ptr != value) {
                std::shared_ptr<V> old_value = value_ptr;

                map.at(key_ptr) = std::make_shared<V>(std::move(value));
                change.fire(typename Event::Update(key_ptr.get(), old_value.get(), value_ptr.get()));
            }
            return value_ptr.get();
        }
    }

    std::optional<V> remove(K const &key) const override {
        if (map.count(key) > 0) {
            std::shared_ptr<V> old_value = map.at(key);
            change.fire(typename Event::Remove(&key, old_value.get()));
            map.erase(key);
            return std::move(*old_value);
        }
        return std::nullopt;
    }

    void clear() const override {
        std::vector<Event> changes;
        for (auto const &[key, value] : map) {
            changes.push_back(typename Event::Remove(key.get(), value.get()));
        }
        for (auto const &it : changes) {
            change.fire(it);
        }
        map.clear();
    }

    size_t size() const override {
        return map.size();
    }

    bool empty() const override {
        return map.empty();
    }
};

static_assert(std::is_move_constructible_v<ViewableMap<int, int> >);

#endif //RD_CPP_CORE_VIEWABLE_MAP_H

//
// Created by jetbrains on 10.07.2018.
//

#ifndef RD_CPP_CORE_VIEWABLE_MAP_H
#define RD_CPP_CORE_VIEWABLE_MAP_H


#include "Logger.h"
#include "IViewableMap.h"
#include "SignalX.h"
#include "util/core_util.h"

#include "tsl/ordered_map.h"

template<typename K, typename V>
class ViewableMap : public IViewableMap<K, V> {
public:
    using Event = typename IViewableMap<K, V>::Event;
private:
    using WK = typename IViewableMap<K, V>::WK;
    using WV = typename IViewableMap<K, V>::WV;

    mutable tsl::ordered_map<rd::Wrapper<K>, rd::Wrapper<V>, rd::TransparentHash<K>, rd::TransparentKeyEqual<K>> map;
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
        /*for (auto const &[key, value] : map) {*/
        for (auto const &it : map) {
            auto &key = it.first;
            auto &value = it.second;
            handler(Event(typename Event::Add(&(*key), &(*value))));;
        }
    }

    const V *get(K const &key) const override {
        auto it = map.find(key);
        if (it == map.end()) {
            return nullptr;
        }
        return &(*it->second);
    }

    const V *set(WK key, WV value) const override {
        if (map.count(key) == 0) {
            /*auto[it, success] = map.emplace(std::make_unique<K>(std::move(key)), std::make_unique<V>(std::move(value)));*/
            auto node = map.emplace(rd::Wrapper<K>(std::move(key)), rd::Wrapper<V>(std::move(value)));
            auto &it = node.first;
            auto const &key_ptr = it->first;
            auto const &value_ptr = it->second;
            change.fire(typename Event::Add(&(*key_ptr), &(*value_ptr)));
            return nullptr;
        } else {
            auto it = map.find(key);
            auto const &key_ptr = it->first;
            auto const &value_ptr = it->second;

            if (*value_ptr != rd::get<V>(value)) {//todo more effective
                rd::Wrapper<V> old_value = std::move(map.at(key));

                map.at(key_ptr) = rd::Wrapper<V>(std::move(value));
                change.fire(typename Event::Update(&(*key_ptr), &(*old_value), &(*value_ptr)));
            }
            return &*(value_ptr);
        }
    }

    tl::optional<WV> remove(K const &key) const override {
        if (map.count(key) > 0) {
            rd::Wrapper<V> old_value = std::move(map.at(key));
            change.fire(typename Event::Remove(&key, &(*old_value)));
            map.erase(key);
            return rd::unwrap<V>(std::move(old_value));
        }
        return tl::nullopt;
    }

    void clear() const override {
        std::vector<Event> changes;
        /*for (auto const &[key, value] : map) {*/
        for (auto const &it : map) {
            changes.push_back(typename Event::Remove(&(*it.first), &(*it.second)));
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

static_assert(std::is_move_constructible<ViewableMap<int, int> >::value,
              "Is move constructible from ViewableMap<int, int>");

#endif //RD_CPP_CORE_VIEWABLE_MAP_H

//
// Created by jetbrains on 13.08.2018.
//

#ifndef RD_CPP_IVIEWABLELIST_H
#define RD_CPP_IVIEWABLELIST_H

#include "overloaded.h"
#include "interfaces.h"
#include "viewable_collections.h"

#include "mpark/variant.hpp"

#include <unordered_map>
#include <vector>
#include <utility>
#include <algorithm>

/*template<typename T>
class IViewableList;

template<typename T>
std::vector<T> convert_to_list(IViewableList<T> const &list);*/

template<typename T>
class IViewableList : public IViewable<std::pair<size_t, T const *>> {
public:
    class Event {
    public:
        class Add {
        public:
            int32_t index;
            T const *new_value;

            Add(size_t index, T const *new_value) : index(index), new_value(new_value) {}
        };

        class Update {
        public:
            int32_t index;
            T const *old_value;
            T const *new_value;

            Update(size_t index, T const *old_value, T const *new_value) : index(index), old_value(old_value),
                                                                           new_value(new_value) {}
        };

        class Remove {
        public:
            int32_t index;
            T const *old_value;

            Remove(size_t index, T const *old_value) : index(index), old_value(old_value) {}
        };

        mpark::variant<Add, Update, Remove> v;

        Event(Add const &x) : v(x) {}

        Event(Update const &x) : v(x) {}

        Event(Remove const &x) : v(x) {}

        int32_t get_index() const {
            return mpark::visit(make_visitor(
                    [](typename Event::Add const &e) {
                        return e.index;
                    },
                    [](typename Event::Update const &e) {
                        return e.index;
                    },
                    [](typename Event::Remove const &e) {
                        return e.index;
                    }
            ), v);
        }

        T const *get_new_value() const {
            return mpark::visit(make_visitor(
                    [](typename Event::Add const &e) {
                        return e.new_value;
                    },
                    [](typename Event::Update const &e) {
                        return e.new_value;
                    },
                    [](typename Event::Remove const &e) {
                        return static_cast<T const *>(nullptr);
                    }
            ), v);
        }
    };

protected:
    mutable std::unordered_map<Lifetime, std::vector<LifetimeDefinition>> lifetimes;
public:
    //region ctor/dtor

    IViewableList() = default;

    IViewableList(IViewableList &&) = default;

    IViewableList &operator=(IViewableList &&) = default;

    virtual ~IViewableList() = default;
    //endregion

    void advise_add_remove(Lifetime lifetime, std::function<void(AddRemove, size_t, T const &)> handler) const {
        advise(std::move(lifetime), [handler](Event e) {
            mpark::visit(make_visitor(
                    [handler](typename Event::Add const &e) {
                        handler(AddRemove::ADD, e.index, *e.new_value);
                    },
                    [handler](typename Event::Update const &e) {
                        handler(AddRemove::REMOVE, e.index, *e.old_value);
                        handler(AddRemove::ADD, e.index, *e.new_value);
                    },
                    [handler](typename Event::Remove const &e) {
                        handler(AddRemove::REMOVE, e.index, *e.old_value);
                    }
            ), e.v);
        });
    }

    virtual void
    view(Lifetime lifetime,
         std::function<void(Lifetime lifetime, std::pair<size_t, T const *> const &)> handler) const {
        view(std::move(lifetime), [handler](Lifetime lt, size_t idx, T const &v) {
            handler(lt, std::make_pair(idx, &v));
        });
    }

    void view(Lifetime lifetime, std::function<void(Lifetime, size_t, T const &)> handler) const {
        advise_add_remove(lifetime, [this, lifetime, handler](AddRemove kind, size_t idx, T const &value) {
            switch (kind) {
                case AddRemove::ADD: {
                    LifetimeDefinition def(lifetime);
                    std::vector<LifetimeDefinition> &v = lifetimes[lifetime];
                    auto it = v.emplace(v.begin() + idx, std::move(def));
                    handler(it->lifetime, idx, value);
                    break;
                }
                case AddRemove::REMOVE: {
                    LifetimeDefinition def = std::move(lifetimes.at(lifetime)[idx]);
                    std::vector<LifetimeDefinition> &v = lifetimes.at(lifetime);
                    v.erase(v.begin() + idx);
                    def.terminate();
                    break;
                }
            }
        });
    }

    virtual void advise(Lifetime lifetime, std::function<void(Event)> handler) const = 0;

    virtual bool add(T element) const = 0;

    virtual bool add(size_t index, T element) const = 0;

    virtual T removeAt(size_t index) const = 0;

    virtual bool remove(T const &element) const = 0;

    virtual T const &get(size_t index) const = 0;

    virtual T set(size_t index, T element) const = 0;

    virtual bool addAll(size_t index, std::vector<T> elements) const = 0;

    virtual bool addAll(std::vector<T> elements) const = 0;

    virtual void clear() const = 0;

    virtual bool removeAll(std::vector<T> elements) const = 0;

    virtual size_t size() const = 0;

    virtual bool empty() const = 0;

    template<typename U>
    friend std::vector<U> convert_to_list(IViewableList<U> const &list);

protected:
    virtual const std::vector<std::shared_ptr<T>> &getList() const = 0;
};

template<typename T>
std::vector<T> convert_to_list(IViewableList<T> const &list) {
    std::vector<T> res(list.size());
    std::transform(list.getList().begin(), list.getList().end(), res.begin(),
                   [](std::shared_ptr<T> const &ptr) { return *ptr; });
    return res;
}

static_assert(std::is_move_constructible<IViewableList<int>::Event>::value, "Is move constructible from IViewableList<int>::Event");

#endif //RD_CPP_IVIEWABLELIST_H

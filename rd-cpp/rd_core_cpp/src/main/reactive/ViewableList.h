//
// Created by jetbrains on 10.07.2018.
//

#ifndef RD_CPP_CORE_VIEWABLELIST_H
#define RD_CPP_CORE_VIEWABLELIST_H


#include <base/IViewableList.h>
#include "interfaces.h"
#include "SignalX.h"


#include <set>
#include <unordered_set>
#include <algorithm>

template<typename T>
class ViewableList : public IViewableList<T> {
public:
    using Event = typename IViewableList<T>::Event;

    template<typename V, typename S>
    friend
    class RdList;

private:
    mutable std::vector<std::shared_ptr<T> > list;
    Signal<Event> change;

protected:
    virtual const std::vector<std::shared_ptr<T>> &getList() const {
        return list;
    }

public:

    //region ctor/dtor

    ViewableList() = default;

    ViewableList(ViewableList &&) = default;

    ViewableList &operator=(ViewableList &&) = default;

    virtual ~ViewableList() = default;

    //endregion

    void advise(Lifetime lifetime, std::function<void(Event)> handler) const override {
        if (lifetime->is_terminated()) return;
        change.advise(std::move(lifetime), handler);
        for (size_t i = 0; i < size(); ++i) {
            handler(typename Event::Add(i, list[i].get()));
        }
    }

    bool add(T element) const override {
        list.push_back(std::make_shared<T>(std::move(element)));
        change.fire(typename Event::Add(size() - 1, list.back().get()));
        return true;
    }

    bool add(size_t index, T element) const override {
        list.insert(list.begin() + index, std::make_shared<T>(std::move(element)));
        change.fire(typename Event::Add(index, list[index].get()));
        return true;
    }

    T removeAt(size_t index) const override {
        auto res = std::move(list[index]);
        list.erase(list.begin() + index);

        change.fire(typename Event::Remove(index, res.get()));
        return std::move(*res);
    }

    bool remove(T const &element) const override {
        auto it = std::find_if(list.begin(), list.end(), [&element](auto const &p) { return *p == element; });
        if (it == list.end()) {
            return false;
        }
        removeAt(std::distance(list.begin(), it));
        return true;
    }

    T const &get(size_t index) const override {
        return *list[index];
    }

    T set(size_t index, T element) const override {
        auto old_value = std::move(list[index]);
        list[index] = std::make_shared<T>(std::move(element));
        change.fire(typename Event::Update(index, old_value.get(), list[index].get()));//???
        return std::move(*old_value);
    }

    bool addAll(size_t index, std::vector<T> elements) const override {
        for (auto &element : elements) {
            add(index, std::move(element));
            ++index;
        }
        return true;
    }

    bool addAll(std::vector<T> elements) const override {
        for (auto &element : elements) {
            add(std::move(element));
        }
        return true;
    }

    void clear() const override {
        std::vector<Event> changes;
        for (size_t i = size(); i > 0; --i) {
            changes.push_back(typename Event::Remove(i - 1, list[i - 1].get()));
        }
        for (auto const &e : changes) {
            change.fire(e);
        }
        list.clear();
    }

    bool removeAll(std::vector<T> elements) const override { //todo faster
//        std::unordered_set<T> set(elements.begin(), elements.end());

        bool res = false;
        for (size_t i = list.size(); i > 0; --i) {
            if (std::count(elements.begin(), elements.end(), *list[i - 1]) > 0) {
                removeAt(i - 1);
                res = true;
            }
        }
        return res;
    }

    size_t size() const override {
        return list.size();
    }

    bool empty() const override {
        return list.empty();
    }
};

static_assert(std::is_move_constructible<ViewableList<int> >::value, "Is move constructible from ViewableList<int>");

#endif //RD_CPP_CORE_VIEWABLELIST_H

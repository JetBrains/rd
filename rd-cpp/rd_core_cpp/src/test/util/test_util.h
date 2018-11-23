//
// Created by jetbrains on 16.07.2018.
//

#ifndef RD_CPP_CORE_TEST_UTIL_H
#define RD_CPP_CORE_TEST_UTIL_H

#include "ViewableList.h"
#include "ViewableSet.h"
#include "ViewableMap.h"

#include "core_util.h"

#include <vector>

std::string operator "" _s(char const *str, size_t len);


template<typename T>
constexpr std::vector<T> arrayListOf(std::initializer_list<T> args) {
    return std::vector<T>(args);
}

template<typename F, typename S>
std::string to_string(const std::pair<F, S> p) {
    return "(" + to_string(p.first) + ", " + to_string(p.second) + ")";
}

template<typename F, typename S>
std::string to_string(const std::pair<F, S *> p) {
    return "(" + to_string(p.first) + ", " + to_string(*p.second) + ")";
}

template<typename F, typename S>
std::string to_string(const std::pair<F *, S *> p) {
    return "(" + to_string(*p.first) + ", " + to_string(*p.second) + ")";
}

template<typename K, typename V>
std::string to_wstring_map_event(typename IViewableMap<K, V>::Event const &e) {
//    return "";
    using Event = typename IViewableMap<K, V>::Event;
    std::string res = mpark::visit(make_visitor(
            [](typename Event::Add const &e) {
                return "Add " +
                       to_string(*e.key) + ":" +
                       to_string(*e.new_value);
            },
            [](typename Event::Update const &e) {
                return "Update " +
                       to_string(*e.key) + ":" +
//                       to_string(e.old_value) + ":" +
                       to_string(*e.new_value);
            },
            [](typename Event::Remove const &e) {
                return "Remove " +
                       to_string(*e.key);
            }
    ), e.v);
    return res;
}

template<typename T>
std::string to_string_list_event(typename IViewableList<T>::Event const &e) {
//    return "";
    using Event = typename IViewableList<T>::Event;
    std::string res = mpark::visit(make_visitor(
            [](typename Event::Add const &e) {
                return "Add " +
                       to_string(e.index) + ":" +
                       to_string(*e.new_value);
            },
            [](typename Event::Update const &e) {
                return "Update " +
                       to_string(e.index) + ":" +
//                       to_string(e.old_value) + ":" +
                       to_string(*e.new_value);
            },
            [](typename Event::Remove const &e) {
                return "Remove " +
                       to_string(e.index);
            }
    ), e.v);
    return res;
}

#endif //RD_CPP_CORE_TEST_UTIL_H

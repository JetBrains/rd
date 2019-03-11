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

namespace rd {
	namespace test {
		namespace util {
			using namespace rd::util;
			using namespace std::string_literals;

			template<typename T0, typename ...T>
			constexpr std::vector<T0> arrayListOf(T0 &&arg, T &&... args) {
				return std::vector<T0>{std::forward<T0>(arg), std::forward<T>(args)...};
			}

			template<typename K, typename V>
			std::string to_string_map_event(typename IViewableMap<K, V>::Event const &e) {
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
				using Event = typename IViewableList<T>::Event;
				std::string res = mpark::visit(make_visitor(
						[](typename Event::Add const &e) {
							return "Add " +
								   std::to_string(e.index) + ":" +
								   to_string(*e.new_value);
						},
						[](typename Event::Update const &e) {
							return "Update " +
								   std::to_string(e.index) + ":" +
								   //                       to_string(e.old_value) + ":" +
								   to_string(*e.new_value);
						},
						[](typename Event::Remove const &e) {
							return "Remove " +
								   std::to_string(e.index);
						}
				), e.v);
				return res;
			}

			template<typename T>
			std::string to_string_set_event(typename IViewableSet<T>::Event const &e) {
				return to_string(e.kind) + " " + to_string(e.value);
			}
		}
	}
}

#endif //RD_CPP_CORE_TEST_UTIL_H

//
// Created by jetbrains on 10.07.2018.
//

#ifndef RD_CPP_CORE_VIEWABLELIST_H
#define RD_CPP_CORE_VIEWABLELIST_H


#include "IViewableList.h"
#include "SignalX.h"
#include "core_util.h"

#include <algorithm>

namespace rd {
	template<typename T>
	class ViewableList : public IViewableList<T> {
	public:
		using Event = typename IViewableList<T>::Event;

		template<typename V, typename S>
		friend
		class RdList;

	private:
		mutable std::vector<Wrapper<T> > list;
		Signal<Event> change;

	protected:
		using WT = typename IViewableList<T>::WT;

		const std::vector<Wrapper<T>> &getList() const override {
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
				handler(typename Event::Add(i, &(*list[i])));
			}
		}

		bool add(WT element) const override {
			list.emplace_back(std::move(element));
			change.fire(typename Event::Add(size() - 1, &(*list.back())));
			return true;
		}

		bool add(size_t index, WT element) const override {
			list.emplace(list.begin() + index, std::move(element));
			change.fire(typename Event::Add(index, &(*list[index])));
			return true;
		}

		WT removeAt(size_t index) const override {
			auto res = std::move(list[index]);
			list.erase(list.begin() + index);

			change.fire(typename Event::Remove(index, &(*res)));
			return wrapper::unwrap<T>(std::move(res));
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

		WT set(size_t index, WT element) const override {
			auto old_value = std::move(list[index]);
			list[index] = std::move(element);
			change.fire(typename Event::Update(index, &(*old_value), &(*list[index])));//???
			return wrapper::unwrap<T>(std::move(old_value));
		}

		bool addAll(size_t index, std::vector<WT> elements) const override {
			for (auto &element : elements) {
				add(index, std::move(element));
				++index;
			}
			return true;
		}

		bool addAll(std::vector<WT> elements) const override {
			for (auto &&element : elements) {
				add(std::move(element));
			}
			return true;
		}

		void clear() const override {
			std::vector<Event> changes;
			for (size_t i = size(); i > 0; --i) {
				changes.push_back(typename Event::Remove(i - 1, &(*list[i - 1])));
			}
			for (auto const &e : changes) {
				change.fire(e);
			}
			list.clear();
		}

		bool removeAll(std::vector<WT> elements) const override { //todo faster
			//        std::unordered_set<T> set(elements.begin(), elements.end());

			bool res = false;
			for (size_t i = list.size(); i > 0; --i) {
				auto const &x = list[i - 1];
				if (std::count_if(elements.begin(), elements.end(), [this, &x](auto const &elem) {
									  return TransparentKeyEqual<T>()(elem, x);
								  }
				) > 0) {
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
}

static_assert(std::is_move_constructible<rd::ViewableList<int> >::value,
			  "Is move constructible from ViewableList<int>");

#endif //RD_CPP_CORE_VIEWABLELIST_H
